package eu.unicore.services.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.persist.PersistenceException;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;

/**
 * For accessing a local registry (running in this container), publishing entries
 * 
 * @author schuller
 */
public class LocalRegistryClient implements IRegistry {

	private final String resID;

	private final String serviceName;

	private final Kernel kernel;

	public LocalRegistryClient(String serviceName, String resID, Kernel kernel)throws Exception {
		this.kernel=kernel;
		this.serviceName = serviceName;
		this.resID = resID;
	} 

	public LocalRegistryClient(Kernel kernel)throws Exception {
		this("Registry", "default_registry", kernel);
	} 

	public String addEntry(String endpoint, Map<String,String>content, Calendar requestedTT) 
	throws Exception {
		try (RegistryImpl reg = (RegistryImpl)getHome().getForUpdate(resID)){
			return reg.addEntry(endpoint, content, requestedTT);
		}
		finally {
			invalidateCache();
		}
	}
	
	public void refresh(String endpoint, Map<String,String>content) throws Exception {
		try (RegistryImpl reg = (RegistryImpl)getHome().getForUpdate(resID)){
			reg.refresh(endpoint, content);
		}
		finally {
			invalidateCache();
		}
	}
	
	public List<Map<String,String>> listEntries() throws Exception {
		List<Map<String,String>> res = getCached();
		if(res!=null) {
			return res;
		}
		res = new ArrayList<>();
		RegistryImpl reg = null;
		if(kernel.getMessaging().hasMessages(resID)){
			reg = (RegistryImpl)getHome().refresh(resID);
		}
		else{
			reg = get();
		}
		for(String entryID: reg.getModel().getEntryIDs()){
			res.add(getEntry(entryID).getModel().getContent());
		}
		cache(res);
		return res;
	}

	private RegistryImpl get() throws PersistenceException {
		return (RegistryImpl)(getHome().get(resID));
	}
	
	private RegistryEntryImpl getEntry(String entryID) throws PersistenceException {
		return (RegistryEntryImpl)(kernel.getHome(RegistryEntryImpl.SERVICENAME).get(entryID));
	}

	private Home getHome(){
		Home home = kernel.getHome(serviceName);
		if(home==null)throw new IllegalStateException("No Registry service deployed");
		return home;
	}

	private synchronized List<Map<String,String>> getCached(){
		Long upd = updated.get(resID);
		if(upd!=null && System.currentTimeMillis()<upd+cacheTime) {
			return cache.get(resID);
		}
		return null;
	}

	private synchronized void cache(List<Map<String,String>> entries) {
		cache.put(resID, entries);
		updated.put(resID, System.currentTimeMillis());
	}

	public synchronized void invalidateCache() {
		cache.remove(resID);
		updated.remove(resID);
	}

	private static final long cacheTime = 5 * 60 * 1000;
	private static final Map<String,List<Map<String,String>>>cache = new HashMap<>();
	private static final Map<String,Long>updated= new HashMap<>();

}
