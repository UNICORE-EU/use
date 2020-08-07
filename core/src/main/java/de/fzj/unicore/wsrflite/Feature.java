package de.fzj.unicore.wsrflite;

import java.util.List;
import java.util.Map;

/**
 * Metadata for deploying a USE feature
 *  
 * @author schuller
 */
public interface Feature extends KernelInjectable
{ 
	
	/**
	 * Unique name of the feature. The name is referenced in the container
	 * property files for further configuration of the feature
	 */
	public String getName();
	

	/**
	 * the Home classes that should be deployed and started for this feature 
	 */
	public Map<String, Class<? extends Home>> getHomeClasses();
	
	
	/**
	 * Services for this feature
	 */
	public List<DeploymentDescriptor> getServices();
	
	public Kernel getKernel();
	
	/**
	 * init tasks to be executed after deployment
	 */
	public List<Runnable> getInitTasks();

	public default boolean isEnabled() {
		return true;
	}

	public default boolean isServiceEnabled(String serviceName) {
		return true;
	}
}