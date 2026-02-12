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
import static eu.unicore.services.security.pdp.DefaultPDP.PERMIT_READ;

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
		UnicoreXPDP xpdp = kernel.getSecurityManager().getPdp();
		if(xpdp!=null && xpdp instanceof DefaultPDP) {
			DefaultPDP pdp = (DefaultPDP)xpdp;
			pdp.setServiceRules("registries", Arrays.asList(PERMIT_READ));
			pdp.setServiceRules("registryentries", Arrays.asList(PERMIT_READ));
			pdp.setServiceRules("ServiceGroupEntry", Arrays.asList(PERMIT_READ));
			// configure access to the "default_registry" instance
			final List<Rule> defaultRegistryRules = new ArrayList<>();
			// general read access
			defaultRegistryRules.add(DefaultPDP.PERMIT_READ);
			if (isGlobal) {
				// write access for role "server"
				defaultRegistryRules.add(
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
			pdp.setServiceRules("Registry", defaultRegistryRules);
		}
	}

	private void setupRegistryCrawler(){
		Runnable crawler = ()->{
			try{
				kernel.getAttribute(RegistryHandler.class).updatePublicKeys();
			}catch(Throwable ex){
				Log.logException("", ex, Log.getLogger(Log.UNICORE, RegistryHandler.class));
			}
		};
		crawler.run();
		kernel.getContainerProperties().getThreadingServices().
		getScheduledExecutorService().scheduleAtFixedRate(crawler, 60, 60, TimeUnit.SECONDS);
	}
}