package eu.unicore.services.rest.admin;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;

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
