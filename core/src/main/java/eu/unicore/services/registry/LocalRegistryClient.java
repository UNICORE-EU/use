package eu.unicore.services.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;

/**
 * For accessing a local registry (running in this container), publishing entries
 * 
 * @author schuller
 */
public class LocalRegistryClient{

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
		Home home = getHome();
		RegistryImpl reg = null;
		try{
			reg = (RegistryImpl)home.getForUpdate(resID);
			return reg.addEntry(endpoint, content, requestedTT);
		}finally{
			if(reg!=null)home.persist(reg);
		}
	}
	
	public void refresh(String endpoint) throws Exception {
		Home home = getHome();
		RegistryImpl reg = null;
		try{
			reg = (RegistryImpl)home.getForUpdate(resID);
			reg.refresh(endpoint);
		}finally{
			if(reg!=null)home.persist(reg);
		}
	}
	
	public Collection<Map<String,String>> listEntries() throws Exception {
		Collection<Map<String,String>> res = new ArrayList<>();
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
