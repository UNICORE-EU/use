package de.fzj.unicore.wsrflite.registry.ws;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import de.fzj.unicore.wsrflite.utils.deployment.FeatureImpl;
import de.fzj.unicore.wsrflite.xmlbeans.registry.LocalRegistryEntryHomeImpl;
import de.fzj.unicore.wsrflite.xmlbeans.registry.LocalRegistryHomeImpl;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryHomeImpl;
import de.fzj.unicore.wsrflite.xmlbeans.sg.ServiceGroupEntry;
import de.fzj.unicore.wsrflite.xmlbeans.sg.ServiceGroupRegistration;
import eu.unicore.services.rest.RegistryServiceDescriptor;
import eu.unicore.services.ws.cxf.CXFService;

/**
 * Registry SOAP web service
 * 
 * Handles both local (default) and shared registry
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
			this.implementationClass = ServiceRegistryEntryHomeImpl.class;
			this.interfaceClass = ServiceGroupEntry.class;
			this.frontendClass = SGEFrontend.class;
		}

		public void setKernel(Kernel kernel){
			super.setKernel(kernel);
		}
	}
}
