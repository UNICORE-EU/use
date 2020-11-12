package de.fzj.unicore.wsrflite.registry;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.messaging.ResourceDeletedMessage;
import eu.unicore.util.Log;

/**
 * base service registry implementation
 * 
 * @author schuller
 */
public class ServiceRegistryEntryImpl extends ResourceImpl {
	
	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", ServiceRegistryEntryImpl.class);
	
	public static final String SERVICENAME="ServiceGroupEntry";
	
	@Override
	public ServiceRegistryEntryModel getModel(){
		return (ServiceRegistryEntryModel)model;
	}
	
	@Override
	public void initialise(InitParameters initParams) throws Exception {
		if(model==null){
			setModel(new ServiceRegistryEntryModel());
		}
		super.initialise(initParams);
		ServiceRegistryEntryInitParameters init = (ServiceRegistryEntryInitParameters)initParams;
		ServiceRegistryEntryModel m = getModel();
		m.setEndpoint(init.endpoint);
	}

	@Override
	public void destroy() {
		try{
			String parent = getModel().getParentUID();
			getKernel().getMessaging().getChannel(parent).publish(new ResourceDeletedMessage(getUniqueID()));
		}
		catch(Exception e){
			Log.logException("Could not send notification.",e,logger);
		}
		super.destroy();
	}
	
}
