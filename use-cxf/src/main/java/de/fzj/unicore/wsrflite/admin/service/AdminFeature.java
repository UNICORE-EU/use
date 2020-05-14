package de.fzj.unicore.wsrflite.admin.service;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.FeatureImpl;

/**
 * Admin service (SOAP and REST)
 * 
 * @author schuller
 */
public class AdminFeature extends FeatureImpl {

	public AdminFeature(Kernel kernel) {
		this();
		setKernel(kernel);
	}
	
	public AdminFeature() {
		this.name = "Admin";
	}
	
	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
	}

	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		
		services.add(new AdminServiceDescriptor(kernel));		
		services.add(new eu.unicore.services.rest.AdminServiceDescriptor(kernel));		
		
		return services;
	}
	
}
