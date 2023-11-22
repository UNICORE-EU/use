package eu.unicore.uas.security.xuudb;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import eu.unicore.security.SubjectAttributesHolder;

/**
 * cache for authz info
 * 
 * @author schuller
 */
public class CredentialCache {

	private final Cache<Object, SubjectAttributesHolder> cache;
	
	/**
	 * create credential cache
	 */
	public CredentialCache(){
		this(10);
	}
	
	/**
	 * create credential cache with the specified entry lifetime
	 * 
	 * @param timeToLive - seconds after which an cache entry expires
	 */
	public CredentialCache(int timeToLive){
		cache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(timeToLive, TimeUnit.SECONDS)
				.expireAfterWrite(timeToLive, TimeUnit.SECONDS)
				.build();
	}

	
	public SubjectAttributesHolder read(Object key){
		return cache.getIfPresent(key);
	}
	
	public void put(Object key, SubjectAttributesHolder authzInfo){
		cache.put(key,authzInfo);
	}
	
	public void removeAll(){
		cache.invalidateAll();
	}

	public long getCacheSize() {
		return cache.size();
	}
}
