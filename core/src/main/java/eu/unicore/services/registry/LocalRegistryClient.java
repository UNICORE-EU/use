package eu.unicore.services.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.persist.PersistenceException;
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
	}
	
	public void refresh(String endpoint) throws Exception {
		try (RegistryImpl reg = (RegistryImpl)getHome().getForUpdate(resID)){
			reg.refresh(endpoint);
		}
	}
	
	public List<Map<String,String>> listEntries() throws Exception {
		List<Map<String,String>> res = new ArrayList<>();
		RegistryImpl reg = null;
		if(kernel.getMessaging().hasMessages(resID)){
			reg = (RegistryImpl)getHome().refresh(resID);
		}
		else{
			reg = get();
		}
		res.addAll(reg.getModel().getContents().values());
		return res;
	}

	private RegistryImpl get() throws PersistenceException {
		return (RegistryImpl)(getHome().get(resID));
	}
	
	private Home getHome(){
		Home home = kernel.getHome(serviceName);
		if(home==null)throw new IllegalStateException("No Registry service deployed");
		return home;
	}
}
