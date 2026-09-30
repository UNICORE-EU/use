package eu.unicore.services.rest.admin;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;
import jakarta.ws.rs.core.Application;

/**
 * Admin service feature
 * 
 * @author schuller
 */
public class AdminFeature extends FeatureImpl {

	public AdminFeature() {
		super("Admin");
	}

	@Override
	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		services.add(new AdminServiceDescriptor(kernel));
	}

	public static class AdminRESTApplication extends Application implements USERestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<Class<?>>();
			classes.add(Admin.class);
			return classes;
		}
	}

	public static class AdminServiceDescriptor extends DeploymentDescriptorImpl {

		public AdminServiceDescriptor(Kernel kernel){
			this();
			setKernel(kernel);
		}

		public AdminServiceDescriptor(){
			super();
			this.name = "admin";
			this.type = RestService.TYPE;
			this.implementationClass = AdminRESTApplication.class;
		}
	}

}
