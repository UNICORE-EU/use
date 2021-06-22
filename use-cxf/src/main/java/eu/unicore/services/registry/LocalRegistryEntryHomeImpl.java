package eu.unicore.services.registry;

import java.util.Collection;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.util.Log;

public class LocalRegistryEntryHomeImpl extends DefaultHome {

	/**
	 * setup the expiry check
	 * Local registry entries do not expire but become refreshed when they reach 
	 * their termination time.
	 */
	@Override
	protected void initExpiryCheck(Collection<String> uniqueIDs)throws Exception{
		super.initExpiryCheck(uniqueIDs);
		instanceChecking.removeChecker(expiryChecker);
		expiryChecker = new RegistryEntryUpdater();
		instanceChecking.addChecker(expiryChecker);
	}
		
	@Override
	protected Resource doCreateInstance() {
		return new RegistryEntryImpl();
	}

	@Override
	public void runExpiryCheckNow(){
		super.runExpiryCheckNow();
		try{
			for(String id: serviceInstances.getUniqueIDs()){
				try{
					//forces refresh of registry entry in global registry
					expiryChecker.process(this, id);
				}catch(Exception ex){
					Log.logException("Error refreshing registry entry.", ex, logger);
				}
			}
		}catch(PersistenceException pe){
			throw new RuntimeException(pe);
		}
	}

}
