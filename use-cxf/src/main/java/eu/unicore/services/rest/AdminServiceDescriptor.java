package eu.unicore.services.rest;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;

public class AdminServiceDescriptor extends DeploymentDescriptorImpl {

	public AdminServiceDescriptor(Kernel kernel){
		this();
		setKernel(kernel);
	}
	
	public AdminServiceDescriptor(){
		super();
		this.name = "admin";
		this.type = RestService.TYPE;
		this.implementationClass = AdminApplication.class;
	}
	
}
