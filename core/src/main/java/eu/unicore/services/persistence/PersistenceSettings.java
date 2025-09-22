package eu.unicore.services.persistence;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

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

	private final static Logger logger = Log.getLogger(Log.UNICORE, PersistenceSettings.class);

	private List<String>concurrentMethods = new ArrayList<>();

	public PersistenceSettings(){}

	public List<String>getConcurrentMethodNames(){
		return concurrentMethods;
	}

	/**
	 * create persistence settings for the given service class, 
	 * by evaluating annotations
	 *
	 * @param spec
	 * @return PersistenceSettings to be used for the service
	 */
	public static PersistenceSettings get(Class<?> spec){
		logger.debug("Generating persistence settings for class {}", spec.getName());
		PersistenceSettings persistenceSettings = new PersistenceSettings();
		persistenceSettings.concurrentMethods = findConcurrentMethods(spec);
		logger.trace("Persistence settings for {}", ()-> persistenceSettings.toString());
		return persistenceSettings;
	}

	/**
	 * returns true if access to the named method can be concurrent
	 * @see ConcurrentAccess
	 * @param methodName - the name of the Method
	 */
	public boolean isConcurrentMethod(String methodName){
		return concurrentMethods.contains(methodName);
	}

	/**
	 * returns true if access to the given method can be concurrent
	 * @see ConcurrentAccess
	 * @param method - the Method
	 */
	public boolean isConcurrentMethod(Method method){
		return concurrentMethods.contains(method.getName());
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(super.toString());
		sb.append(" concurrent methods="+concurrentMethods.toString());
		return sb.toString();
	}

	public static List<String> findConcurrentMethods(Class<?> spec){
		List<String>concurrentMethods = new ArrayList<String>();
		if(!spec.getSuperclass().equals(Object.class)){
			concurrentMethods = findConcurrentMethods(spec.getSuperclass());
		}
		Method[] methods = spec.getDeclaredMethods();
		for(Method m:methods){
			ConcurrentAccess access = m.getAnnotation(ConcurrentAccess.class);
			if(access!=null){
				boolean val = access.allow();
				if(val)
					concurrentMethods.add(m.getName());
				else
					concurrentMethods.remove(m.getName());
			}
		}
		return concurrentMethods;
	}

}
