package eu.unicore.services.rest.admin;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Kernel;
import eu.unicore.services.utils.deployment.FeatureImpl;

/**
 * Admin service
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
		return services;
	}
	
}
