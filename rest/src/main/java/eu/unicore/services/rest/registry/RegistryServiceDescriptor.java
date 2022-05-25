package eu.unicore.services.rest.registry;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;

public class RegistryServiceDescriptor extends DeploymentDescriptorImpl {

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
