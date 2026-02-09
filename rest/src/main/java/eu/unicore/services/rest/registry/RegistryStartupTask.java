package eu.unicore.services.rest.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryCreator;
import eu.unicore.services.security.pdp.DefaultPDP;
import eu.unicore.services.security.pdp.DefaultPDP.Rule;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.pdp.UnicoreXPDP;
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
			setupAccessPolicy(registryCreator.isGlobalRegistry());
			kernel.getHome(RegistryCreator.SERVICE_NAME).addPublicResourceID(RegistryCreator.DEFAULT_REGISTRY_ID);
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void setupAccessPolicy(boolean isGlobal){
		UnicoreXPDP pdp = kernel.getSecurityManager().getPdp();
		if(pdp!=null && pdp instanceof DefaultPDP) {
			DefaultPDP dPDP = (DefaultPDP)pdp;
			dPDP.setServiceRules("registries", Arrays.asList(DefaultPDP.PERMIT_READ));
			dPDP.setServiceRules("registryentries", Arrays.asList(DefaultPDP.PERMIT_READ));
			dPDP.setServiceRules("ServiceGroupEntry", Arrays.asList(DefaultPDP.PERMIT_READ));
			final List<Rule> rRules = new ArrayList<>();
			// general read access
			rRules.add(DefaultPDP.PERMIT_READ);
			if (isGlobal) {
				// write access for role "server"
				rRules.add(
						(client, action, resource) -> {
							if(RegistryCreator.DEFAULT_REGISTRY_ID.equals(resource.getResourceID())
									&& client!=null 
									&& client.getRole()!=null 
									&& "server".equals(client.getRole().getName()))
							{
								return Decision.PERMIT;
							}
							return Decision.UNCLEAR;
						}
				);		
			}
			dPDP.setServiceRules("Registry", rRules);
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