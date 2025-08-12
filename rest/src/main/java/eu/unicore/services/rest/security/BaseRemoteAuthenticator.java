package eu.unicore.services.rest.security;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.BasicAttributeHolder;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ConnectionUtil;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Base class for authenticating with a remote server<br/>
 * 
 * The credentials are extracted from the incoming message, and authentication is
 * performed with the configured endpoint.<br>
 * Valid assertions are cached for a time, either the validity period specified
 * in the authentication result, or a default time of 5 minutes.<br/>
 *
 * Aside from assigning the user's identity, the remote authenticator can be configured
 * to also set authorization attributes (uid, role, groups). This is intended to simplify
 * common use cases. The attributes assigned here can be overriden later with the
 * configured attribute sources.
 * 
 * @author schuller 
 */
public abstract class BaseRemoteAuthenticator<T> implements IAuthenticator, KernelInjectable, ExternalSystemConnector {

	private static final Logger logger = Log.getLogger(Log.SECURITY, BaseRemoteAuthenticator.class);

	protected String address;

	protected Kernel kernel;

	// just scheme://host+port for checking connections
	protected String simpleAddress;

	// usually, we do not want to use the server's certificate for authn calls,
	// since it might cause to wrongly authenticate the user as the server
	protected boolean doTLSAuthN = false;

	protected final CircuitBreaker cb = new CircuitBreaker();

	protected Cache<Object,CacheEntry<T>> cache;

	// how long to cache "failed" authentication results (millis)
	protected static long defaultCacheTime =  5 * 60 * 1000;

	// translation scripts / attributes assignment
	protected String identityAssign;
	protected String uidAssign;
	protected String roleAssign;
	protected String groupsAssign;

	public void setKernel(Kernel kernel){
		this.kernel = kernel;
		createCache();
		selfCheck();
	}

	protected void selfCheck() throws ConfigurationException {
		if(address==null) {
			throw new ConfigurationException(getClass().getName()+": Parameter 'address' is required");
		}
	}

	public void setAddress(String address) {
		this.address = address;
		try {
			URL u = new URL(address);
			simpleAddress = u.getProtocol()+"://"+u.getAuthority();
		}catch(Exception e) {
			this.simpleAddress = address;
		}
	}

	public String getAddress() {
		return address;
	}
	
	public void setDoTLSAuthn(boolean use) {
		this.doTLSAuthN = use;
	}

	public void setIdentityAssign(String identityAssign) {
		this.identityAssign = handleAssignScript(identityAssign, "identityAssign");
	}

	public void setUidAssign(String uidAssign) {
		this.uidAssign = handleAssignScript(uidAssign, "uidAssign");
	}

	public void setRoleAssign(String roleAssign) {
		this.roleAssign = handleAssignScript(roleAssign, "roleAssign");
	}

	public void setGroupsAssign(String groupsAssign) {
		this.groupsAssign = handleAssignScript(groupsAssign, "groupsAssign");
	}

	@Override
	public String toString(){
		return address;
	}

	private String handleAssignScript(String script, String paramName) {
		if(script!=null && script.startsWith("@")) {
			try{
				return FileUtils.readFileToString(new File(script.substring(1)), "UTF-8");
			}catch(IOException io) {
				throw new ConfigurationException("Cannot read value for '"+paramName+"'", io);
			}
		}
		else{
			return script;
		}
	}

	@Override
	public final boolean authenticate(Message message, SecurityTokens tokens) {
		DefaultClientConfiguration clientCfg = kernel.getClientConfiguration();
		clientCfg.setSslAuthn(doTLSAuthN);
		Object cacheKey = extractCredentials(clientCfg, message, tokens);
		if(cacheKey == null)return false;
		
		CacheEntry<T> ce = cache.getIfPresent(cacheKey);
		boolean cacheHit = ce!=null && !ce.expired();
		T auth = cacheHit? ce.auth : null;
		try{
			if(auth==null){
				if(!cb.isOK()) {
					// have credentials, but can't check
					return true;
				}
				auth = performAuth(clientCfg);
				long expires = getExpiryTime(auth);
				cache.put(cacheKey, new CacheEntry<T>(auth,expires));
			}
			Map<String,Object> attr = extractAttributes(auth);
			String dn = assignIdentity(auth, attr);
			if(dn!=null){
				logger.debug("Successfully authenticated (cached: {}) via {}: <{}>", cacheHit, this, dn);
				tokens.setUserName(dn);
				tokens.setConsignorTrusted(true);
				tokens.getContext().put(AuthNHandler.USER_AUTHN_METHOD, getAuthNMethod());
				try {
					BasicAttributeHolder bah = assignAttributes(attr);
					if(bah!=null) {
						tokens.getContext().put(AuthAttributesCollector.ATTRIBUTES, bah);
						logger.debug("Extracted attributes: {}", bah);
					}
				}catch(Exception ex) {
					logger.debug("Error extracting attributes from {}: {}", address, ex.getMessage());
				}
			}
		}catch(Exception ex){
			logger.debug("Error authenticating at {}: {}", address, ex.getMessage());
		}
		return true;
	}

	/**
	 * returns the name of the authentication method supported by this authenticator
	 */
	protected abstract String getAuthNMethod();

	/**
	 * extract credentials from the incoming message, and setup the client configuration 
	 * accordingly. If the message does not contain appropriate credentials, 
	 * returns <code>null</code>, otherwise, returns a non-null object suitable for
	 * use as a key for the assertion cache.
	 * 
	 * @param clientCfg
	 * @param message
	 * @param tokens
	 * @return an object suitable as key for the credential cache - e.g. the user DN,
	 *         bearer token,... or <code>null</code> if there is no authentication
	 *         material in the message
	 */
	protected abstract Object extractCredentials(DefaultClientConfiguration clientCfg, Message message, SecurityTokens tokens);

	/**
	 * call the external service for authentication and return the result
	 * 
	 * (Note: this method should handle any communication problems by setting 
	 * the circuit breaker to notOK())
	 */
	protected abstract T performAuth(DefaultClientConfiguration clientCfg) throws Exception;

	/**
	 * assign identity based on the authentication - like the DN and any TD stuff
	 *
	 * @param auth
	 * @param attrs attributes extracted via extractAttributes()
	 * @return user identity (X500name) or <code>null</code> if not authenticated
	 */
	protected abstract String assignIdentity(T auth, Map<String, Object>attrs);

	/**
	 * extract attributes (uid, role, groups) from the auth reply
	 * @param auth
	 * @return map with attributes
	 */
	protected abstract Map<String, Object> extractAttributes(T auth);

	/**
	 * assign attributes based on the configured attribute assignment scripts
	 */
	protected BasicAttributeHolder assignAttributes(Map<String, Object> attr) {
		if(uidAssign==null&&roleAssign==null&&groupsAssign==null)return null;
		BasicAttributeHolder bah = new BasicAttributeHolder();
		if(uidAssign!=null) {
			bah.uid = RESTUtils.evaluateToString(uidAssign, attr);
		}
		if(roleAssign!=null) {
			bah.setRole(RESTUtils.evaluateToString(roleAssign, attr));
		}
		if(groupsAssign!=null) {
			bah.groups = RESTUtils.evaluateToArray(groupsAssign, attr);
		}
		return bah;
	}

	protected long getExpiryTime(T authObject){
		return System.currentTimeMillis() + defaultCacheTime;
	}

	static class CacheEntry<T> {
		public T auth;
		public long expires;
		public CacheEntry(T auth,long expires){
			this.expires = expires;
			this.auth = auth;
		}
		public boolean expired(){
			return System.currentTimeMillis()>expires;
		}
	}

	public void createCache() {
		cache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(300, TimeUnit.SECONDS)
				.expireAfterWrite(300, TimeUnit.SECONDS)
				.build();
	}

	private Status status=Status.UNKNOWN;
	private String statusMessage;
	private long lastChecked;

	@Override
	public String getConnectionStatusMessage(){
		checkConnection();	
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus(){
		checkConnection();
		return status;
	}

	private void checkConnection(){
		if (lastChecked+2000>System.currentTimeMillis())
			return;
		ContainerProperties conf = kernel.getContainerProperties();
		try {
			Pair<Boolean, String> result = TimeoutRunner.compute(getCheckConnectionTask(address), conf.getThreadingServices(), 2000);
			if(result!=null && result.getM1()){
				status=Status.OK;
				statusMessage="OK [connected to "+simpleAddress+"]";
			}
			else {
				status=Status.DOWN;
				statusMessage = result!=null? result.getM2() : "CAN'T CONNECT to "+simpleAddress;
			}
		}catch(Exception e) {
			status=Status.UNKNOWN;
			statusMessage = Log.createFaultMessage("ERROR checking status", e);
		}
		lastChecked=System.currentTimeMillis();
	}

	private Callable<Pair<Boolean,String>> getCheckConnectionTask(final String url) {
		return new Callable<>(){
			public Pair<Boolean, String> call() {
				try {
					DefaultClientConfiguration clientCfg = kernel.getClientConfiguration();
					if(!url.toLowerCase().startsWith("https")) {
						clientCfg.setSslEnabled(false);
						URL u = new URL(url);
						String host = u.getHost();
						int port = u.getPort();
						if(port==-1)port = u.getDefaultPort();
						try(Socket s = new Socket(host,port)){}
					}
					else {
						ConnectionUtil.getPeerCertificate(clientCfg, url, 2000, logger);
					}
					return new Pair<>(Boolean.TRUE, "OK");
				} catch (UnknownHostException e) {
					return new Pair<>(Boolean.FALSE, "Host is unknown: " + e);
				} catch (IOException e) {
					return new Pair<>(Boolean.FALSE, String.format("Can't contact %s: %s", url, e));
				}
			}
		};
	}
}
