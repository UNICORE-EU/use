package eu.unicore.services.registry;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.InitParameters;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.messaging.Message;
import eu.unicore.util.Log;

/**
 * basis registry entry implementation
 * 
 * @author schuller
 */
public class RegistryEntryImpl extends ResourceImpl {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryEntryImpl.class);

	public static final String SERVICENAME="ServiceGroupEntry";

	@Override
	public RegistryEntryModel getModel(){
		return (RegistryEntryModel)model;
	}

	@Override
	public void initialise(InitParameters initParams) throws Exception {
		if(model==null){
			setModel(new RegistryEntryModel());
		}
		super.initialise(initParams);
		RegistryEntryInitParameters init = (RegistryEntryInitParameters)initParams;
		getModel().setContent(init.content);
	}

	@Override
	public void destroy() {
		try{
			String parent = getModel().getParentUID();
			getKernel().getMessaging().getChannel(parent).publish(new Message(getUniqueID()));
		}
		catch(Exception e){
			Log.logException("Could not send notification.",e,logger);
		}
		super.destroy();
	}

}