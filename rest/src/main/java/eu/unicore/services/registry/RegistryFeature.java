package eu.unicore.services.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.registry.RegistryServiceDescriptor;
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
		getInitTasks().add(new RegistryStartupTask(kernel));
		
	}

	@Override
	public Map<String, Class<? extends Home>> getHomeClasses(){
		Map<String, Class<? extends Home>> homeClasses = new HashMap<>();
		if(isSharedRegistry) {
			homeClasses.put("Registry", RegistryHomeImpl.class);
			homeClasses.put("ServiceGroupEntry", RegistryEntryHomeImpl.class);
		}
		else {
			homeClasses.put("Registry", LocalRegistryHomeImpl.class);
			homeClasses.put("ServiceGroupEntry", LocalRegistryEntryHomeImpl.class);
		}
		return homeClasses;
	}
	
	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		services.add(new RegistryServiceDescriptor(kernel));		
		return services;
	}

}
