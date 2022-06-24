package eu.unicore.services.rest.admin;

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
		services.add(new AdminServiceDescriptor(kernel));
	}

}
