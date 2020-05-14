package de.fzj.unicore.wsrflite.admin.service;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import de.fzj.unicore.wsrflite.xmlbeans.AdminService;
import eu.unicore.services.ws.cxf.CXFService;

public class AdminServiceDescriptor extends DeploymentDescriptorImpl {


	public AdminServiceDescriptor(Kernel kernel){
		this();
		setKernel(kernel);
	}
	
	public AdminServiceDescriptor(){
		super();
		this.name = "AdminService";
		this.type = CXFService.TYPE;
		this.implementationClass = AdminServiceHomeImpl.class;
		this.interfaceClass = AdminService.class;
	}
	
}
