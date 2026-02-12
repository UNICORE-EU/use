package eu.unicore.services.registry;

import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.util.Log;

/**
 * Utilities for dealing with the registry.
 * 
 *  - Initializes the singleton instance of the registry service. Depending on the deployed
 *    Registry home implementation it is either global or local registry.
 *
 *  - allows to force a refresh of all the registry entries
 *
 * @author K. Benedyczak
 * @author B. Schuller
 */
public class RegistryCreator {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryCreator.class);

	public static final String DEFAULT_REGISTRY_ID = "default_registry";
	public static final String SERVICE_NAME = "Registry";
	public static final String ENTRIES_SERVICE_NAME = "ServiceGroupEntry";

	private final boolean isGlobalRegistry; 

	private final Kernel kernel;

	public RegistryCreator(Kernel kernel) {
		this.kernel = kernel;
		Home regHome=kernel.getHome(SERVICE_NAME);
		if (regHome !=null && regHome instanceof RegistryHomeImpl){
			isGlobalRegistry = true;
		}
		else{
			isGlobalRegistry = false;
		}
	}

	public boolean isGlobalRegistry() {
		return isGlobalRegistry;
	}

	public boolean haveRegistry() {
		return kernel.getHome(SERVICE_NAME)!=null;
	}

	public void createRegistry() throws PersistenceException {
		Home regHome = kernel.getHome(SERVICE_NAME);
		if(regHome==null) {
			logger.info("No Registry service configured for this site.");
			return;
		}
		LockSupport ls = kernel.getPersistenceManager().getLockSupport();
		Lock regLock = ls.getOrCreateLock(RegistryCreator.class.getName());
		if(regLock.tryLock()){
			try{
				//check if default registry already exists
				try{
					regHome.get(DEFAULT_REGISTRY_ID);
				}catch(ResourceUnknownException e){
					try {
						createRegistryInstance(regHome);
					} catch (Exception ex) {
						logger.warn("Could not start up registry!",ex);
					}
				}
			}
			finally{
				regLock.unlock();
			}
		}
	}

	private void createRegistryInstance(Home regHome)throws ResourceNotCreatedException{
		regHome.createResource(new InitParameters(DEFAULT_REGISTRY_ID, TerminationMode.NEVER));
		logger.debug("Added '{}' resource to service '{}'", DEFAULT_REGISTRY_ID, SERVICE_NAME);
	}

	public void refreshRegistryEntries() {
		try {
			logger.info("Refreshing registry entries.");
			kernel.getHome(ENTRIES_SERVICE_NAME).runExpiryCheckNow();
		}catch (Exception ex) {
			Log.logException("Error refreshing <"+ENTRIES_SERVICE_NAME+">", ex, logger);
		}
	}

	public static synchronized RegistryCreator get(Kernel kernel) {
		RegistryCreator rc = kernel.getAttribute(RegistryCreator.class);
		if(rc==null) {
			rc = new RegistryCreator(kernel);
			kernel.setAttribute(RegistryCreator.class, rc);
		}
		return rc;
	}

}
