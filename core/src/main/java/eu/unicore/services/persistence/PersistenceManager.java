package eu.unicore.services.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

public class PersistenceManager {

	private static final Logger logger=Log.getLogger(Log.PERSISTENCE,PersistenceManager.class);

	private Class<?> persistClass=null;

	private boolean haveInit=false;

	final Map<String,Store> persistMap = new HashMap<>();

	private final Map<Class<?>,PersistenceSettings> persistenceSettings = Collections.synchronizedMap(new HashMap<>());

	private LockSupport lockSupport;

	private final Kernel kernel;
	
	public PersistenceManager(Kernel kernel){
		this.kernel=kernel;
	}

	/**
	 * initialise persistence manager: load persistence class configured via 
	 * container property {@link ContainerProperties#PERSIST_CLASSNAME}
	 */
	synchronized void init(){
		if(!haveInit){
			haveInit=true;
			try{
				String clazz=kernel.getContainerProperties().getValue(ContainerProperties.PERSIST_CLASSNAME);
				if(clazz.startsWith("de.fzj.unicore.wsrflite")) {
					clazz = clazz.replace("de.fzj.unicore.wsrflite", "eu.unicore.services");
				}
				persistClass=Class.forName(clazz);
				logger.info("Using '{}' for permanent storage.", persistClass.getName());
			}
			catch(Exception e){
				throw new ConfigurationException(Log.createFaultMessage(
						"Error configuring persistence! Please check the configuration and the latest documentation.", e), e);
			}			
		}	
	}

	/**
	 * gets the {@link Store} instance for the given service
	 * 
	 * @param serviceName - the name of the service
	 * @return the {@link Store} implementation configured for the given service
	 */
	public synchronized Store getPersist(String serviceName){
		if(persistClass==null){
			init();
		}
		Store p=persistMap.get(serviceName);
		if (p==null){
			try {
				p=(Store)persistClass.getConstructor().newInstance();	
				p.init(kernel,serviceName);
				persistMap.put(serviceName,p);
			} catch (Exception e) {
				throw new RuntimeException("Cannot create instance of persistence class",e);
			}
		}
		return p;	
	}

	/**
	 * retrieve the persistence settings for a given service class
	 * @param service - the service class
	 */
	public synchronized PersistenceSettings getPersistenceSettings(Class<?> service){
		PersistenceSettings ps=persistenceSettings.get(service);
		if(ps==null){
			ps=PersistenceSettings.get(service);
			persistenceSettings.put(service, ps);
		}
		return ps;
	}

	/**
	 * removes the {@link Store} instance of a stateful service from the PersistenceManager.<br>
	 * @param serviceName - the service name
	 */
	public void removePersist(String serviceName) {
		persistMap.remove(serviceName);
	}

	/**
	 * get helper for dealing with locks (a singleton)
	 * @return {@link LockSupport}
	 */
	public synchronized LockSupport getLockSupport(){
		if(lockSupport==null){
			lockSupport=new LockSupport("__use_internal__");
		}
		return lockSupport;
	}	
}
