package eu.unicore.services.registry;

import java.util.Calendar;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.util.Log;

/**
 * base service registry implementation
 * 
 * @author schuller
 */
public class RegistryImpl extends ResourceImpl {
	
	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryImpl.class);
	
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
				logger.debug("Refreshed registry entry for <{}>", endpoint);
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
			logger.info("Creating new registry entry for <{}>",endpoint);
			tt = pushToExternalRegistries(endpoint, content, tt);
			entryID = createSREntry(tt, endpoint);
		}
		getModel().put(endpoint, entryID, content);
		return entryID;
	}
	
	protected String createSREntry(Calendar tt, String endpoint) throws ResourceNotCreatedException {
		RegistryEntryInitParameters sgInit = new RegistryEntryInitParameters(tt);
		sgInit.parentUUID = this.getUniqueID();
		sgInit.parentServiceName = this.getServiceName();
		sgInit.endpoint = endpoint;
		Home sreHome=getKernel().getHome(RegistryEntryImpl.SERVICENAME);
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
	public RegistryModel getModel(){
		return (RegistryModel)model;
	}
	
	@Override
	public void initialise(InitParameters initParams) throws Exception {
		if(model==null){
			setModel(new RegistryModel());
		}
		super.initialise(initParams);
	}

	@Override
	public void processMessages(PullPoint p) {
		try{
			while(p.hasNext()){
				String id=(String)p.next().getBody();
				logger.debug("Removing entry with id={}", id);
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
		logger.debug("Updating information for <{}>", endpoint);
		Home sreHome=getKernel().getHome(RegistryEntryImpl.SERVICENAME);
		String entryUID = getModel().getEntryID(endpoint);
		Map<String,String> content = getModel().getContent(endpoint);
		RegistryEntryImpl wsr=(RegistryEntryImpl)sreHome.getForUpdate(entryUID);
		try{
			RegistryEntryModel model = wsr.getModel();
			model.setEndpoint(endpoint);
			Calendar tt = pushToExternalRegistries(endpoint, content, null);
			if(tt==null)tt=getDefaultEntryLifetime();
			wsr.setTerminationTime(tt);
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
	
	// flags an entry as internal, so it won't get pushed to
	// any external registries
	public static final String MARK_ENTRY_AS_INTERNAL = "InternalEntry";
	
}
