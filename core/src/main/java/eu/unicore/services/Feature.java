package eu.unicore.services;

import java.util.List;
import java.util.Map;

/**
 * Metadata for deploying a USE feature
 *
 * @author schuller
 */
public interface Feature extends KernelInjectable{

	/**
	 * Unique name of the feature. The name is referenced in the container property
	 * files for further configuration of the feature
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
	 * to be executed after feature deployment
	 */
	public void initialise() throws Exception;

	/**
	 * init tasks to be executed after all services are deployed (end of Kernel
	 * start())
	 */
	public List<StartupTask> getStartupTasks();

	public default boolean isEnabled() {
		return true;
	}

	public default boolean isServiceEnabled(String serviceName) {
		return true;
	}

	public default String getVersion() {
		return Kernel.getVersion(this.getClass());
	}

}