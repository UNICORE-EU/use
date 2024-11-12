package eu.unicore.services.rest.registry;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryEntryHomeImpl;
import eu.unicore.services.registry.RegistryHomeImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;


/**
 * Feature definition for both the local (default) and shared registry
 *
 * @author schuller
 */
public class RegistryFeature extends FeatureImpl {

	public static final String SERVICE_NAME = "Registry";

	private RegistryFeatureProperties properties;
	
	private boolean isSharedRegistry = false;
	
	public RegistryFeature() {
		this.name = "Registry";
	}
	
	public boolean isSharedRegistry() {
		return isSharedRegistry;
	}
	
	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		this.properties = new RegistryFeatureProperties(SERVICE_NAME, kernel.getContainerProperties());
		this.isSharedRegistry = "shared".equalsIgnoreCase(properties.getValue("mode"));
		if(isSharedRegistry) {
			homeClasses.put("Registry", RegistryHomeImpl.class);
			homeClasses.put("ServiceGroupEntry", RegistryEntryHomeImpl.class);
		}
		else {
			homeClasses.put("Registry", LocalRegistryHomeImpl.class);
			homeClasses.put("ServiceGroupEntry", LocalRegistryEntryHomeImpl.class);
		}
		services.add(new RegistryServiceDescriptor(kernel));		
	}

	@Override
	public void initialise() throws Exception {
		new RegistryStartupTask(kernel).run();
	}

}
