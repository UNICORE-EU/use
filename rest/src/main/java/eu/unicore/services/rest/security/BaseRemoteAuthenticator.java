package eu.unicore.services.rest.security;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Base class for authenticating with a remote server
 * 
 * The credentials are extracted from the incoming message. Assertions are validated 
 * using the container's configured trusted assertion issuers.
 * Valid assertions are cached for some time, usually the validity period specified
 * in the authentication assertion from Unity.
 * 
 * @author schuller 
 */
public abstract class BaseRemoteAuthenticator<T> implements IAuthenticator, KernelInjectable {

	private static final Logger logger = Log.getLogger(Log.SECURITY, BaseRemoteAuthenticator.class);

	protected String address;

	protected Kernel kernel;

	// usually, we do not want to use the server's certificate for authn calls,
	// since it might cause to wrongly authenticate the user as the server
	protected boolean doTLSAuthN = false;
	
	private static final AtomicInteger count = new AtomicInteger();
	
	protected final CircuitBreaker cb = new CircuitBreaker("REST_"+getClass().getSimpleName()+"_"+count.incrementAndGet());

	protected Cache<Object,CacheEntry<T>> cache;
	
	// how long to cache "failed" authentication results (millis)
	protected static long defaultCacheTime =  5 * 60 * 1000;

	public void setKernel(Kernel kernel){
		this.kernel = kernel;
		kernel.getMetricRegistry().register(cb.getName(),cb);
		createCache();
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}
	
	public void setDoTLSAuthn(boolean use) {
		this.doTLSAuthN = use;
	}

	public String toString(){
		return address;
	}
			
	@Override
	public final boolean authenticate(Message message, SecurityTokens tokens) {
		DefaultClientConfiguration clientCfg = kernel.getClientConfiguration();
		clientCfg.setSslAuthn(doTLSAuthN);
		Object cacheKey = extractCredentials(clientCfg, message, tokens);
		if(cacheKey == null)return false;

		if(!cb.isOK())return true;
		
		CacheEntry<T> ce = cache.getIfPresent(cacheKey);
		boolean cacheHit = ce!=null && !ce.expired();
		T auth = cacheHit? ce.auth : null;

		try{
			if(auth==null){
				auth = performAuth(clientCfg);
				long expires = getExpiryTime(auth);
				cache.put(cacheKey, new CacheEntry<T>(auth,expires));
			}
			extractAuthInfo(auth, tokens);
			String dn = tokens.getUserName();
			if(dn!=null){
				logger.debug("Successfully authenticated (cached: {}) via {}: <{}>", cacheHit, this, dn);
			}
		}catch(Exception ex){
			Log.logException("Error authenticating using "+address, ex, logger);
		}
		return true;
	}
	
	/**
	 * extract credentials from the incoming message, and setup the client configuration 
	 * accordingly. If the message does not contain appropriate credentials, 
	 * returns <code>null</code>, otherwise, returns a non-null object suitable for
	 * use as a key for the assertion cache.
	 * 
	 * @param clientCfg
	 * @param message
	 * @param tokens
	 * @return an object suitable as key for the credential cache - e.g. the user DN, bearer token,...
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
	 * extract info for authenticated users - like the DN and any TD stuff
	 * 
	 * @param auth
	 * @param tokens
	 */
	protected abstract void extractAuthInfo(T auth, SecurityTokens tokens);
	
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
}
