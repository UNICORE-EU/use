package eu.unicore.services;

import java.util.Collections;
import java.util.List;

/**
 * Specify deployment metadata for USE services
 *  
 * @author schuller
 */ 
public interface DeploymentDescriptor extends KernelInjectable
{ 
	
	public String getName();
	
	/**
	 * the service type ('rest', 'cxf', ...)
	 */
	public String getType();
	
	/**
	 * the interface class name
	 */
	public Class<?> getInterface();
	
	/**
	 * the implementation class name
	 */
	public Class<?> getImplementation();

	/**
	 * init tasks to be executed after deployment
	 */
	public default List<Runnable> getInitTasks(){
		return Collections.emptyList();
	}

	public Kernel getKernel();
	
	public default boolean isEnabled() {
		return true;
	}

}