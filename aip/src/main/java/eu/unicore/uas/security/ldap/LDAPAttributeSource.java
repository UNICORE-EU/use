package eu.unicore.uas.security.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.canl.SSLContextCreator;
import eu.unicore.uas.security.vo.UnicoreAttributesHandler;
import eu.unicore.uas.security.xuudb.CredentialCache;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * get authorisation attributes for users by asking an LDAP
 * 
 * General TODO:
 *  - support all UNICORE incarnation attributes (addDefaultGids, perUserQueue, 
 *  primary vs supplementary gids, LDAP group as VO)
 *  - No way to specify a default value for attribute, if more then one valid value is allowed (e.g. xlogin).
 *  - handle referral
 *  - setup SASL authN
 *  
 * The first two points should be solved by using a common code with the SAML AIP, which implements this using 
 * {@link UnicoreAttributesHandler} and friends.
 * 
 * @author valley
 * @author delaruelle
 */
public class LDAPAttributeSource implements IAttributeSource {

	private static final Logger logger=Log.getLogger(Log.SECURITY,LDAPAttributeSource.class);
	public static final int DEFAULT_PORT= 389;
	public static final String DEFAULT_HOST="ldap://localhost";
	public static final String DEFAULT_ROOTDN="";
	public static final String DEFAULT_LDAPFILTER="";
	public static final String DEFAULT_AUTHENTICATION="none";
	public static final String DEFAULT_PRINCIPAL="";
	public static final String DEFAULT_CREDENTIAL="";
	public static final String DEFAULT_DNATTRNAME="";
	public static final String DEFAULT_LOGINATTRNAME="";
	
	public static final String DEFAULT_ROLEATTRNAME="";
	public static final String DEFAULT_ROLEDEFAULTVALUE="";
	public static final String DEFAULT_PROJECTATTRNAME="";
	public static final String DEFAULT_PROJECTDEFAULTVALUE="";
	
	public static final int DEFAULT_LDAP_MAX_CONNECT_RETRY = 3;

	private String name;
	private Kernel kernel;
	private boolean isEnabled = false;
	private Integer port;
	//host contains also the protocol section of the URI (ldap/ldaps) 
	private String host;
	private String rootdn;
	private String ldapfilter;
	private String authentication;
	private String principal;
	private String credentials;
	private String DNAttrName;
	private String loginAttrName;
	private String roleAttrName;
	private String roleDefaultValue;
	private String groupAttrName;
	private String groupDefaultValue;
	private boolean cacheCredentials=true;
	private Integer maxConnectionRetry;

	private String ldapURL=null;
	private DirContext ldap;
	private CredentialCache cache;

	private JMXStats jmxStats = new JMXStats();

	@Override
	public void configure(String name) throws ConfigurationException
	{
		this.name = name;
		if (port == null)
			port = DEFAULT_PORT;
		if (host == null)
			host = DEFAULT_HOST;
		if (rootdn == null)
			rootdn = DEFAULT_ROOTDN;
		if (ldapfilter == null)
			ldapfilter = DEFAULT_LDAPFILTER;
		if (authentication == null)
			authentication = DEFAULT_AUTHENTICATION;
		if (principal == null)
			principal = DEFAULT_PRINCIPAL;
		if (credentials == null)
			credentials = DEFAULT_CREDENTIAL;
		if (DNAttrName == null)
			DNAttrName = DEFAULT_DNATTRNAME;
		if (loginAttrName == null)
			loginAttrName = DEFAULT_LOGINATTRNAME;
		if (roleAttrName == null)
			roleAttrName = DEFAULT_ROLEATTRNAME;
		if (roleDefaultValue == null)
			roleDefaultValue = DEFAULT_ROLEDEFAULTVALUE;
		if (groupDefaultValue == null)
			groupDefaultValue = DEFAULT_PROJECTDEFAULTVALUE;
		if (maxConnectionRetry == null)
			maxConnectionRetry = DEFAULT_LDAP_MAX_CONNECT_RETRY;
	}

	@Override
	public void start(Kernel kernel)
	{
		this.kernel = kernel;
		ldapURL = host + ":" + port + "/" ;
		logger.info("LDAP attribute source '" + name +
				"': connecting to LDAP at <"+ldapURL+">");
		if(cacheCredentials)
			logger.debug("LDAP " + name + " will cache credentials.");
		isEnabled = true;
		try {
			ldap = makeEndpoint();
		} catch (NamingException e) {
			Log.logException("Error in LDAP connection.",e,logger);
		}
		cache = new CredentialCache();
	}

	// --------- configuration injection --------------
	
	public void setEndpoint(DirContext ldap){
		this.ldap=ldap;
	}

	public void setLdapPort(int port) {
		this.port = port;
	}

	public void setLdapHost(String host) {
		this.host = host;
	}

	public void setLdapRootDn(String rootdn) {
		this.rootdn = rootdn;
	}

	public void setLdapFilter(String filter) {
		this.ldapfilter = filter;
	}

	public void setLdapAuthentication(String authentication) {
		this.authentication = authentication;
	}

	public void setLdapPrincipal(String principal) {
		this.principal = principal;
	}

	public void setLdapCredential(String credentials) {
		this.credentials = credentials;
	}

	public void setLdapDNAttrName(String name) {
		this.DNAttrName = name;
	}

	public void setLdapLoginAttrName(String name) {
		this.loginAttrName = name;
	}

	public void setLdapRoleAttrName(String name) {
		this.roleAttrName = name;
	}

	public void setLdapRoleDefaultValue(String value) {
		this.roleDefaultValue = value;
	}

	public void setLdapGroupAttrName(String name) {
		this.groupAttrName = name;
	}

	public void setLdapGroupDefaultValue(String value) {
		this.groupDefaultValue = value;
	}

	public void setLdapCache(boolean cache) {
		this.cacheCredentials = cache;
	}

	public void setLdapMaxConnectionsRetry(int retry) {
		this.maxConnectionRetry = retry;
	}

	// --------- configuration injection END --------------
	
	
	
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException
	{
		long begin=System.currentTimeMillis();
		jmxStats.incTotalAuth();
		
		String cacheKey = X500NameUtils.getComparableForm(tokens.getEffectiveUserName());
		SubjectAttributesHolder map = getCachingCredentials()? cache.read(cacheKey) : null;
		
		if (map==null) {
			map=checkDN(tokens);
			if(getCachingCredentials())
				cache.put(tokens.toString(),map);
		} else {
			if(getCachingCredentials())
				jmxStats.incCacheHits();
		}
		jmxStats.publishTime(System.currentTimeMillis()-begin);
		return map;
	}

	/**
	 * retrieves user attributes from LDAP
	 * @param tokens
	 * @return SubjectAttributesHolder
	 */
	protected SubjectAttributesHolder checkDN(final SecurityTokens tokens)throws IOException{

		SubjectAttributesHolder map = null;

		String dn=tokens.getEffectiveUserName();
		jmxStats.addAccessor(dn);
		//build ldap filter : DN match + filter settings
		String filter = "("+DNAttrName+"="+dn+")";
		if (ldapfilter != "") 
			filter = "(&" + filter +  ldapfilter + ")";
		if(logger.isDebugEnabled()){
			logger.debug("LDAP request: "+filter);
		}
		
		String[] returnAttrs = { loginAttrName, roleAttrName, groupAttrName };
		SearchControls ctls = new SearchControls();
		ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		ctls.setReturningAttributes(returnAttrs);
		ctls.setReturningObjFlag(true);
		NamingEnumeration<SearchResult> enm = null;
		int retryNb = 0;
		while (true) {
			synchronized(this){
				try {
					if (ldap != null)
					{
						// Search for objects that have those matching attributes
						enm = ldap.search(this.rootdn, filter, ctls);
						map = makeAuthInfo(enm);
						break;
					}
				} catch(NamingException e){
					Log.logException("Error in LDAP request.",e,logger);
					retryNb++;
					//ends with exception if there are too much retries
					if (retryNb >= maxConnectionRetry) {
						IOException ioe=new IOException("Error contacting LDAP: "+e.getMessage());
						ioe.initCause(e);
						throw ioe;
					}
				}
				
				//We try to reconnect to LDAP server and then retry
				while (true) {
					try {
						ldap = makeEndpoint();
						break;
					} catch (NamingException e) {
						Log.logException("Error in LDAP connection.",e,logger);
						retryNb++;
						//ends with exception if there are too much retries
						if (retryNb >= maxConnectionRetry) {
							IOException ioe=new IOException("Error contacting LDAP: "+e.getMessage());
							ioe.initCause(e);
							throw ioe;
						}
					}
				}
			}
		}
		return map;
	}

	/**
	 ** parse search result from ldap and return a map of auth info
	 * @throws NamingException 
	 **/
	public SubjectAttributesHolder makeAuthInfo(NamingEnumeration<SearchResult> enm) throws NamingException {
		SearchResult res;
		Attributes resAttrs;
		List<String> role = new ArrayList<String>();
		List<String> xlogin = new ArrayList<String>();
		List<String> group = new ArrayList<String>();

		//in case null enumeration
		if (enm == null) {
			//return empty SubjectAttributesHolder
			return new SubjectAttributesHolder(new HashMap<String,String[]>(), new HashMap<String,String[]>());
		}
		//loop for each ldap results
		while (enm.hasMore()) {
			try {
				res=enm.next();
				//get attributes of one result
				resAttrs = res.getAttributes();
				//get the login
				if (resAttrs.get(loginAttrName) != null) {
					for (int i=0;i<resAttrs.get(loginAttrName).size();i++) {
						xlogin.add(resAttrs.get(loginAttrName).get(i).toString());
					}
				}
				//get the role(s) associated
				if (resAttrs.get(roleAttrName) != null) {
					for (int i=0;i<resAttrs.get(roleAttrName).size();i++) {
						role.add(resAttrs.get(roleAttrName).get(i).toString());
					}
				} else {
					role.add(roleDefaultValue);
				}
				//get the groups(s) associated
				if (resAttrs.get(groupAttrName) != null) {
					for (int i=0;i<resAttrs.get(groupAttrName).size();i++) {
						group.add(resAttrs.get(groupAttrName).get(i).toString());
					}
				} else {
					group.add(groupDefaultValue);
				}
				//we don't manage referral for the moment...
			} catch (javax.naming.PartialResultException e) {
				Log.logException("LDAP Referral available",e,logger);
			}
		}
		if(logger.isDebugEnabled()){
			String reply="[xlogin="+xlogin+", role="+role+", groups="+group+"]";
			logger.debug("LDAP reply: "+reply);
		}
		Map<String,String[]> map=new HashMap<String,String[]>();
		Map<String,String[]> mapDef=new HashMap<String,String[]>();
		if (xlogin.size() > 0) {
			map.put(IAttributeSource.ATTRIBUTE_XLOGIN,(String[]) xlogin.toArray(new String[xlogin.size()]));
			mapDef.put(IAttributeSource.ATTRIBUTE_XLOGIN,new String[] {xlogin.get(0)});
		}
		if (role.size() > 0) {
			map.put(IAttributeSource.ATTRIBUTE_ROLE,(String[]) role.toArray(new String[role.size()]));
			mapDef.put(IAttributeSource.ATTRIBUTE_ROLE,new String[] {role.get(0)});
		}
		if (group.size() > 0) {
			map.put(IAttributeSource.ATTRIBUTE_GROUP,(String[]) group.toArray(new String[group.size()]));
			mapDef.put(IAttributeSource.ATTRIBUTE_GROUP,new String[] {group.get(0)});
		}
		return new SubjectAttributesHolder(mapDef, map);
	}

	public int getLDAPPort() {
		return port;
	}
	
	public String getLDAPHost() {
		return host;
	}
	
	public String getRootDN() {
		return rootdn;
	}
	
	public synchronized boolean getCachingCredentials(){
		return cacheCredentials;
	}
	
	@Override
	public String getStatusDescription(){
		if(!isEnabled)
			return "No LDAP configured";

		//make a small ldap connection test : search for the first level of root DN
		DirContext testCnx;
		SearchControls ctls = new SearchControls();
		ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		try {
			testCnx = makeEndpoint();
			testCnx.search(this.rootdn, "objectClass=*", ctls);
		} catch(NamingException e){
			return "LDAP Attribute Source ERROR ["+name+" connected to "+ldapURL+"]";
		}
		return "LDAP Attribute Source OK ["+name+" connected to "+ldapURL+"]";
	}
	
	@Override
	public String getName() {
		return name;
	}

	// ----------------------- Utils func -----------------------
	
	private DirContext makeEndpoint() throws NamingException {

		Hashtable<String,String> env = new Hashtable<String,String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, host+":"+port);
		env.put(Context.SECURITY_AUTHENTICATION, authentication);
		if (isUsingSSL()) {
			X509CertChainValidator validator = kernel.getClientConfiguration().getValidator();
			SocketFactory sFactory;
			if ("simple".equals(authentication) || "none".equals(authentication)) {
				//if we use ssl and a none/simple authentication, we have to disable x509 client auth on socketfactory
				sFactory = getCanlSocketFactory(null, validator);
			} else {
				X509Credential credential = kernel.getClientConfiguration().getCredential();
				sFactory = getCanlSocketFactory(credential, validator);
			}
			CanlJndiSocketFactory.setImplementation(sFactory);

			env.put(Context.SECURITY_PROTOCOL, "ssl");
			env.put("java.naming.ldap.factory.socket", CanlJndiSocketFactory.class.getName());
		}
		if ("simple".equals(authentication)) {
			env.put(Context.SECURITY_PRINCIPAL, principal);
			env.put(Context.SECURITY_CREDENTIALS, credentials);
		}
		// Ignore referral
		env.put(Context.REFERRAL, "ignore");

		return new InitialDirContext(env);
	}
	
	private boolean isUsingSSL() {
		return getLDAPHost().startsWith("ldaps");
	}
	
	private SocketFactory getCanlSocketFactory(X509Credential credential, X509CertChainValidator validator) 
			throws NamingException {
		SSLContext ctx;
		try
		{
			ctx = SSLContextCreator.createSSLContext(credential, validator, "TLS", 
					"LDAP Client", logger);
		} catch (Exception e)
		{
			logger.debug("Problem with TLS setup" + e.toString(), e);
			throw new NamingException("Problem setting up TLS infrastructure: " + e.toString());
		}
		return ctx.getSocketFactory();
	}

}
