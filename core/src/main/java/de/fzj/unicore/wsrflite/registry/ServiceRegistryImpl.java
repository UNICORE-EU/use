package de.fzj.unicore.wsrflite.registry;

import java.util.Calendar;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import eu.unicore.util.Log;

/**
 * base service registry implementation
 * 
 * @author schuller
 */
public class ServiceRegistryImpl extends ResourceImpl {
	
	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", ServiceRegistryImpl.class);
	
	/**
	 * create a new entry (or update an existing one) and return the entry ID
	 */
	public String addEntry(String endpoint, Map<String,String>content, Calendar requestedTT) throws ResourceNotCreatedException {
		Calendar tt = requestedTT!=null? requestedTT : getDefaultEntryLifetime();
		String entryID = getModel().getEntryID(endpoint);
		boolean createNew = true;
		content.put(ENDPOINT, endpoint);
		if(entryID!=null)
		{
			try {
				createNew = false;
				refresh(endpoint);
				if(logger.isDebugEnabled())logger.debug("Refreshed registry entry for: <"+endpoint+">");
			} catch (ResourceUnknownException e) {
				// sgEntry has been destroyed in between, so remove it from Entry RP
				getModel().removeEntry(endpoint);
				createNew = true;
			} catch (Exception e){
				//can't update, just warn
				Log.logException("Can't update service registry entry!",e);
			}
		}
		if(createNew){
			logger.info("Creating new registry entry for service: <"+endpoint+">");
			tt = pushToExternalRegistries(endpoint, content, tt);
			entryID = createSREntry(tt, endpoint);
		}
		getModel().put(endpoint, entryID, content);
		return entryID;
	}
	
	protected String createSREntry(Calendar tt, String endpoint) throws ResourceNotCreatedException {
		ServiceRegistryEntryInitParameters sgInit = new ServiceRegistryEntryInitParameters(tt);
		sgInit.parentUUID = this.getUniqueID();
		sgInit.parentServiceName = this.getServiceName();
		sgInit.endpoint = endpoint;
		Home sreHome=getKernel().getHome(ServiceRegistryEntryImpl.SERVICENAME);
		return sreHome.createResource(sgInit);
	}
	
	/**
	 * Push the entry to any external registries.
	 * By default, this does nothing.
	 * @return requested refresh time from external registry (if applicable)
	 */
	protected Calendar pushToExternalRegistries(String endpoint, Map<String,String>content, Calendar requestedTT){
		return requestedTT;
	}
	
	@Override
	public ServiceRegistryModel getModel(){
		return (ServiceRegistryModel)model;
	}
	
	@Override
	public void initialise(InitParameters initParams) throws Exception {
		if(model==null){
			setModel(new ServiceRegistryModel());
		}
		super.initialise(initParams);
	}

	@Override
	public void processMessages(PullPoint p) {
		try{
			while(p.hasNext()){
				String id=(String)p.next().getBody();
				logger.debug("Removing entry with id="+id);
				getModel().removeChild(id);
			}
		}
		catch(Exception e){
			Log.logException("Could not remove entry.",e,logger);
		}
	}
	
	public Calendar getDefaultEntryLifetime()	{
		ContainerProperties cfg = getKernel().getContainerProperties(); 
		long defaultTerminationTime = cfg.getLongValue(ContainerProperties.WSRF_SGENTRY_TERMINATION_TIME);
		Calendar tt = Calendar.getInstance();
		tt.setTimeInMillis(System.currentTimeMillis()+defaultTerminationTime*1000);
		return tt;
	}
	
	public void refresh(String endpoint)throws Exception{
		if(logger.isDebugEnabled())logger.debug("Updating information for <"+endpoint+">");
		Home sreHome=getKernel().getHome(ServiceRegistryEntryImpl.SERVICENAME);
		String entryUID = getModel().getEntryID(endpoint);
		Map<String,String> content = getModel().getContent(endpoint);
		ServiceRegistryEntryImpl wsr=(ServiceRegistryEntryImpl)sreHome.getForUpdate(entryUID);
		Calendar tt = pushToExternalRegistries(endpoint, content, null);
		if(tt==null)tt=getDefaultEntryLifetime();
		try{
			wsr.setTerminationTime(tt);
			ServiceRegistryEntryModel model = wsr.getModel();
			model.setEndpoint(endpoint);
		}
		finally{
			getKernel().getPersistenceManager().persist(wsr);
		}
	}
	

	// constants for storing info in the content map
	public static final String ENDPOINT = "Endpoint";
	public static final String INTERFACE_NAME = "InterfaceName";
	public static final String INTERFACE_NAMESPACE = "InterfaceNamespace";
	public static final String SERVER_IDENTITY = "ServerIdentity";
	public static final String SERVER_PUBKEY = "ServerPublicKey";
	

}
