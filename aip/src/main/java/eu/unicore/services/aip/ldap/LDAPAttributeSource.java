package eu.unicore.services.aip.ldap;

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

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.canl.SSLContextCreator;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.aip.saml.UnicoreAttributesHandler;
import eu.unicore.services.aip.xuudb.CredentialCache;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.utils.CircuitBreaker;
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
public class LDAPAttributeSource implements IAttributeSource, ExternalSystemConnector {

	private static final Logger logger=Log.getLogger(Log.SECURITY,LDAPAttributeSource.class);

	private String name;
	private Kernel kernel;

	private String rootdn = "";
	private String ldapfilter = "";
	private String authentication = "none";
	private String principal = "";
	private String credentials = "";
	private String DNAttrName = "";
	private String loginAttrName = "username";
	private String roleAttrName = "role";
	private String roleDefaultValue = "";
	private String groupAttrName = "projects";
	private String groupDefaultValue = "";

	private String ldapURL = "ldap://localhost:389";
	private String ctxFactory = "com.sun.jndi.ldap.LdapCtxFactory";
	private CredentialCache cache;

	private Status status = Status.UNKNOWN;
	private String statusMessage;
	private final CircuitBreaker cb = new CircuitBreaker();

	@Override
	public void configure(String name, Kernel kernel) throws ConfigurationException
	{
		this.name = name;
		this.kernel = kernel;
		logger.info("LDAP attribute source '{}': connecting to LDAP at <{}", name, ldapURL);
		try {
			makeEndpoint();
		} catch (NamingException e) {
			Log.logException("Error in LDAP connection.",e,logger);
			cb.notOK();
		}
		cache = new CredentialCache();
	}

	// --------- configuration injection --------------

	public void setCtxFactory(String ctxFactory){
		this.ctxFactory = ctxFactory;
	}

	public void setLdapURL(String ldapURL) {
		this.ldapURL = ldapURL;
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

	// --------- configuration injection END --------------

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException
	{
		checkConnection();
		if(!cb.isOK())
			throw new SubsystemUnavailableException("Attribute source "+name+" is temporarily unavailable");
		String cacheKey = X500NameUtils.getComparableForm(tokens.getEffectiveUserName());
		SubjectAttributesHolder map = cache.read(cacheKey);
		if (map==null) {
			try{
				map=checkDN(tokens);
			}catch(IOException e) {
				cb.notOK();
				throw e;
			}
			cache.put(tokens.toString(),map);
		}
		return map;
	}

	/**
	 * retrieves user attributes from LDAP
	 * @param tokens
	 * @return SubjectAttributesHolder
	 */
	private SubjectAttributesHolder checkDN(final SecurityTokens tokens)throws IOException{
		String dn = tokens.getEffectiveUserName();
		//build ldap filter : DN match + filter settings
		String filter = "("+DNAttrName+"="+dn+")";
		if (ldapfilter != "") {
			filter = "(&" + filter +  ldapfilter + ")";
		}
		logger.debug("LDAP request: {}", filter);
		String[] returnAttrs = { loginAttrName, roleAttrName, groupAttrName };
		SearchControls ctls = new SearchControls();
		ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		ctls.setReturningAttributes(returnAttrs);
		ctls.setReturningObjFlag(true);
		synchronized(this){
			try {
				DirContext ldap = makeEndpoint();
				NamingEnumeration<SearchResult> enm = ldap.search(this.rootdn, filter, ctls);
				return makeAuthInfo(enm);
			} catch(NamingException e){
				throw new IOException(e);
			}
		}
	}

	/**
	 ** parse search result from ldap and return a map of auth info
	 * @throws NamingException 
	 **/
	public SubjectAttributesHolder makeAuthInfo(NamingEnumeration<SearchResult> enm) throws NamingException {
		SearchResult res;
		Attributes resAttrs;
		List<String> role = new ArrayList<>();
		List<String> xlogin = new ArrayList<>();
		List<String> group = new ArrayList<>();

		//in case null enumeration
		if (enm == null) {
			//return empty SubjectAttributesHolder
			return new SubjectAttributesHolder(new HashMap<>(), new HashMap<>());
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
		logger.debug("LDAP reply: [xlogin={}, role={}, groups={}]", xlogin, role, group);
		Map<String,String[]> map = new HashMap<>();
		Map<String,String[]> mapDef = new HashMap<>();
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

	@Override
	public Status getConnectionStatus(){
		return status;
	}

	@Override
	public String getExternalSystemName() {
		return name;
	}

	@Override
	public String getConnectionStatusMessage(){
		checkConnection();	
		return statusMessage;
	}

	private void checkConnection() {
		//make a small ldap connection test : search for the first level of root DN
		DirContext testCnx;
		SearchControls ctls = new SearchControls();
		ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		try {
			testCnx = makeEndpoint();
			testCnx.search(this.rootdn, "objectClass=*", ctls);
			status=Status.OK;
			statusMessage = "OK ["+name+" connected to "+ldapURL+"]";
			cb.OK();
		} catch(Exception e){
			statusMessage = Log.createFaultMessage("ERROR", e);
			status = Status.DOWN;
			cb.notOK();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	// ----------------------- Utils func -----------------------
	
	DirContext makeEndpoint() throws NamingException {
		Hashtable<String,String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, ctxFactory);
		env.put(Context.PROVIDER_URL, ldapURL);
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
		return ldapURL!=null && ldapURL.startsWith("ldaps");
	}

	private SocketFactory getCanlSocketFactory(X509Credential credential, X509CertChainValidator validator) 
			throws NamingException {
		SSLContext ctx;
		try
		{
			ctx = SSLContextCreator.createSSLContext(credential, validator, "TLS", 
					"LDAP Client", logger, 
					kernel.getClientConfiguration().getServerHostnameCheckingMode());
		} catch (Exception e)
		{
			Log.logException("Problem setting up TLS", e, logger);
			throw new NamingException(Log.createFaultMessage("Problem setting up TLS", e));
		}
		return ctx.getSocketFactory();
	}

	public String toString() {
		return getName()+" ["+ldapURL+"]";
	}
}
