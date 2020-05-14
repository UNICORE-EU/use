package eu.unicore.services.rest;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;

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
