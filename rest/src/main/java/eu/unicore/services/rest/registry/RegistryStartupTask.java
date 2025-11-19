package eu.unicore.services.rest.registry;

import java.util.concurrent.TimeUnit;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryCreator;
import eu.unicore.util.Log;


/**
 * Creates registry instance and sets up Registry-related tasks
 * 
 * @author K. Benedyczak
 */
public class RegistryStartupTask implements Runnable {

	private final Kernel kernel;

	public RegistryStartupTask(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void run() {
		try {
			RegistryCreator registryCreator = RegistryCreator.get(kernel);
			registryCreator.createRegistry();
			RegistryHandler.get(kernel);
			if (!registryCreator.isGlobalRegistry()) {
				registryCreator.refreshRegistryEntries();
			}
			setupRegistryCrawler();
			kernel.getHome(RegistryCreator.SERVICE_NAME).addPublicResourceID(RegistryCreator.DEFAULT_REGISTRY_ID);
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void setupRegistryCrawler(){
		Runnable command = ()->{
			try{
				kernel.getAttribute(RegistryHandler.class).updatePublicKeys();
			}catch(Throwable ex){
				Log.logException("", ex, Log.getLogger(Log.UNICORE, RegistryHandler.class));
			}
		};
		command.run();
		kernel.getContainerProperties().getThreadingServices().
		getScheduledExecutorService().scheduleAtFixedRate(command, 60, 60, TimeUnit.SECONDS);
	}
}
