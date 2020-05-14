package eu.unicore.uas.security.xuudb;

import java.io.ByteArrayInputStream;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.util.Log;

/**
 * cache for authz info
 * 
 * @author schuller
 */
public class CredentialCache {

	private static final Logger log=Log.getLogger(Log.SECURITY,CredentialCache.class);
	
	private CacheManager cacheManager;
	
	private net.sf.ehcache.Cache cache;
	
	/**
	 * default config for ehcache</br>
	 * This is an in-memory cache only, 
	 * using the default LRU policy 
	 */
	private String defaultConfig;
	
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
		defaultConfig="<ehcache name=\"__xuudb_credential_cache__\">\n" +
		   "<defaultCache maxElementsInMemory=\"100\"\n"+
	        "eternal=\"false\"\n"+
	        "timeToIdleSeconds=\""+timeToLive+"\"\n"+
	        "timeToLiveSeconds=\""+timeToLive+"\"\n"+
	        "overflowToDisk=\"false\"\n"+
	        "diskPersistent=\"false\"\n"+
	        "diskExpiryThreadIntervalSeconds=\"120\"/>\n"+
	        "</ehcache>";

		if(cacheManager==null)initCacheManager();
		if(!cacheManager.cacheExists(CredentialCache.class.getName()))
			cacheManager.addCache(CredentialCache.class.getName());
		cache=cacheManager.getCache(CredentialCache.class.getName());
	}
	
	private synchronized void initCacheManager(){
		try{
			ByteArrayInputStream bis=new ByteArrayInputStream(defaultConfig.getBytes());
			cacheManager=CacheManager.create(bis);
		} catch (CacheException e) {
			log.fatal("Error creating cache manager.",e);
		}		
	}
	
	public SubjectAttributesHolder read(Object key){
		try{
			Element e=cache.get(key);
			if(e!=null){
				return (SubjectAttributesHolder)(e.getObjectValue());
			}
		}catch(Exception e){
			log.warn("",e);
		}
		return null;
	}
	
	public void put(Object key, SubjectAttributesHolder authzInfo){
		try{
			cache.put(new Element(key,authzInfo));
		}catch(Exception e){
			log.warn("",e);
		}
	}
	
	public void removeAll(){
		try{
			cache.removeAll();
		}catch (Exception e) {
			log.warn("Could not clear cache.",e);
		}
	}

	public Cache getCache(){
		return cache;
	}

}
