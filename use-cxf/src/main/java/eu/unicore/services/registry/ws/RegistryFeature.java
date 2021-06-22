package eu.unicore.services.registry.ws;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.LocalRegistryEntryHomeImpl;
import eu.unicore.services.registry.LocalRegistryHomeImpl;
import eu.unicore.services.registry.RegistryEntryHomeImpl;
import eu.unicore.services.registry.RegistryHomeImpl;
import eu.unicore.services.rest.RegistryServiceDescriptor;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;
import eu.unicore.services.ws.cxf.CXFService;
import eu.unicore.services.ws.sg.ServiceGroupEntry;
import eu.unicore.services.ws.sg.ServiceGroupRegistration;

/**
 * Feature definition for both the local (default) and shared registry
 *
 * @author schuller
 */
public class RegistryFeature extends FeatureImpl {

	private RegistryFeatureProperties properties;
	
	private boolean isSharedRegistry = false;
	
	public RegistryFeature(Kernel kernel) {
		this();
		setKernel(kernel);
	}
	
	public RegistryFeature() {
		this.name = "Registry";
	}
	
	public boolean isSharedRegistry() {
		return isSharedRegistry;
	}
	
	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		this.properties = new RegistryFeatureProperties("Registry", kernel.getContainerProperties());
		this.isSharedRegistry = "shared".equalsIgnoreCase(properties.getValue("mode"));
	}

	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		if(isSharedRegistry){
			services.add(new SharedRegistryServiceDescriptor(kernel));
			services.add(new SharedRegistryEntryServiceDescriptor(kernel));		
		}
		else{
			services.add(new LocalRegistryServiceDescriptor(kernel));
			services.add(new LocalRegistryEntryServiceDescriptor(kernel));			
		}
		services.add(new RegistryServiceDescriptor(kernel));		
		
		return services;
	}
	
	
	public static class LocalRegistryServiceDescriptor extends DeploymentDescriptorImpl {

		public LocalRegistryServiceDescriptor(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public LocalRegistryServiceDescriptor() {
			super();
			this.name = "Registry";
			this.type = CXFService.TYPE;
			this.implementationClass = LocalRegistryHomeImpl.class;
			this.interfaceClass = ServiceGroupRegistration.class;
			this.frontendClass = SGFrontend.class;
		}

		public void setKernel(Kernel kernel){
			super.setKernel(kernel);
		}
	}
	
	
	public static class LocalRegistryEntryServiceDescriptor extends DeploymentDescriptorImpl {

		public LocalRegistryEntryServiceDescriptor(Kernel kernel) {
			this();
			setKernel(kernel);
		}
		
		public LocalRegistryEntryServiceDescriptor() {
			super();
			this.name = "ServiceGroupEntry";
			this.type = CXFService.TYPE;
			this.implementationClass = LocalRegistryEntryHomeImpl.class;
			this.interfaceClass = ServiceGroupEntry.class;
			this.frontendClass = SGEFrontend.class;
		}

		public void setKernel(Kernel kernel){
			super.setKernel(kernel);
		}
	}
	
	public static class SharedRegistryServiceDescriptor extends DeploymentDescriptorImpl {
		
		public SharedRegistryServiceDescriptor(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public SharedRegistryServiceDescriptor() {
			super();
			this.name = "Registry";
			this.type = CXFService.TYPE;
			this.implementationClass = RegistryHomeImpl.class;
			this.interfaceClass = ServiceGroupRegistration.class;
			this.frontendClass = SGFrontend.class;
		}

		public void setKernel(Kernel kernel){
			super.setKernel(kernel);
		}
	}
	
	public static class SharedRegistryEntryServiceDescriptor extends DeploymentDescriptorImpl {
		
		public SharedRegistryEntryServiceDescriptor(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public SharedRegistryEntryServiceDescriptor() {
			super();
			this.name = "ServiceGroupEntry";
			this.type = CXFService.TYPE;
			this.implementationClass = RegistryEntryHomeImpl.class;
			this.interfaceClass = ServiceGroupEntry.class;
			this.frontendClass = SGEFrontend.class;
		}

		public void setKernel(Kernel kernel){
			super.setKernel(kernel);
		}
	}
}
