package eu.unicore.services.rest.registry;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryEntryHomeImpl;
import eu.unicore.services.registry.RegistryHomeImpl;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;
import jakarta.ws.rs.core.Application;


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
		services.add(new RegistryServiceEntryDescriptor(kernel));
	}

	@Override
	public void initialise() throws Exception {
		new RegistryStartupTask(kernel).run();
	}

	/**
	 * REST service descriptor for local/shared Registry
	 */
	public static class RegistryServiceDescriptor extends DeploymentDescriptorImpl {

		public RegistryServiceDescriptor(Kernel kernel){
			this();
			setKernel(kernel);
		}

		public RegistryServiceDescriptor(){
			super();
			this.name = "registries";
			this.type = RestService.TYPE;
			this.implementationClass = RegistryApplication.class;
		}
	}
	
	/**
	 * REST application for local/shared Registry
	 */
	public static class RegistryApplication extends Application implements USERestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(Registries.class);
			return classes;
		}

	}
	
	/**
	 * REST service descriptor for registry entries
	 */
	public static class RegistryServiceEntryDescriptor extends DeploymentDescriptorImpl {

		public RegistryServiceEntryDescriptor(Kernel kernel){
			this();
			setKernel(kernel);
		}

		public RegistryServiceEntryDescriptor(){
			super();
			this.name = "registryentries";
			this.type = RestService.TYPE;
			this.implementationClass = RegistryEntryApplication.class;
		}
	}
	
	/**
	 * REST application for registry entries
	 */
	public static class RegistryEntryApplication extends Application implements USERestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(RegistryEntries.class);
			return classes;
		}
	}

}
