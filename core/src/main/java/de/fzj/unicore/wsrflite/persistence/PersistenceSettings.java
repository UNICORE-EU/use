package de.fzj.unicore.wsrflite.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fzj.unicore.persist.Persist;
import eu.unicore.util.ConcurrentAccess;

/**
 * Holds settings that govern the behaviour of the persistence system. 
 * These settings are per service class.
 * 
 * Usually instances of this class are created using {@link PersistenceManager#getPersistenceSettings(Class)}
 * which will create (and cache) the settings
 * 
 * @author schuller
 */
public class PersistenceSettings {
	
	protected final static Logger logger=Logger.getLogger(PersistenceSettings.class);
	
	private boolean loadOnce;
	
	private List<String>concurrentMethods=new ArrayList<String>();
	
	public PersistenceSettings(){}
	
	public PersistenceSettings(boolean loadOnce, Map<String,Field>persistentFields) {
		this.loadOnce=loadOnce;
	}
	
	public List<String>getConcurrentMethodNames(){
		return concurrentMethods;
	}
	
	
	/**
	 * create persistence settings for the given service class, 
	 * by evaluating annotations
	 * 
	 * @see Persist
	 * @see Persistent
	 * 
	 * @param spec
	 * @return PersistenceSettings to be used for the service
	 * 
	 * @since 1.8
	 */
	public static PersistenceSettings get(Class<?> spec){
		if(spec==null)return null;
		logger.debug("Generating persistence settings for class "+spec.getName());
		PersistenceSettings persistenceSettings=new PersistenceSettings();
		boolean loadOnce=false;
		Persistent p=(Persistent)spec.getAnnotation(Persistent.class);
		if(p!=null){
			loadOnce=p.loadSemantics().equals(LoadSemantics.LOAD_ONCE);
		}
		persistenceSettings.loadOnce=loadOnce;
		persistenceSettings.concurrentMethods=findConcurrentMethods(spec);
		if(logger.isTraceEnabled()){
			logger.trace("Persistence settings for "+spec.getName()+"\n"+persistenceSettings.toString());
		}
		return persistenceSettings;
	}
	
	public boolean isLoadOnce(){
		return loadOnce;
	}

	/**
	 * returns true if access to the named method can be concurrent
	 * @see ConcurrentAccess
	 * @param methodName - the name of the Method
	 */
	public boolean isConcurrentMethod(String methodName){
		if(methodName==null)return false;
		return concurrentMethods.contains(methodName);
	}
	
	/**
	 * returns true if access to the given method can be concurrent
	 * @see ConcurrentAccess
	 * @param method - the Method
	 */
	public boolean isConcurrentMethod(Method method){
		if(method==null)return false;
		return concurrentMethods.contains(method.getName());
	}
	
	public String toString(){
		
		StringBuilder sb=new StringBuilder();
		sb.append(super.toString());
		sb.append(" isLoadOnce="+isLoadOnce());
		sb.append(" concurrent methods="+concurrentMethods.toString());
		return sb.toString();
		
	}
	
	public static List<String> findConcurrentMethods(Class<?> spec){
		List<String>concurrentMethods=new ArrayList<String>();
		try{
			if(!spec.getSuperclass().equals(Object.class)){
				concurrentMethods=findConcurrentMethods(spec.getSuperclass());
			}
			Method[] methods=spec.getDeclaredMethods();
			for(Method m:methods){
				ConcurrentAccess access=m.getAnnotation(ConcurrentAccess.class);
				if(access!=null){
					boolean val=access.allow();
					if(val)
						concurrentMethods.add(m.getName());
					else
						concurrentMethods.remove(m.getName());
				}
			}
		}catch(Exception e){
			logger.fatal("Could not create list of concurrent methods",e);
		}
		return concurrentMethods;
	}
	
	public static PersistenceSettings getDefaultSettings(){
		return new PersistenceSettings(false,new HashMap<String, Field>());
	}
}
