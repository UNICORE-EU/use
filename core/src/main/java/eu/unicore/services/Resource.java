package eu.unicore.services;

import java.util.Collection;

import eu.unicore.services.messaging.PullPoint;

/**
 * Life cycle methods and helper methods for Resources. 
 * 
 * These are called by the hosting environment to initialize, destroy or persist
 * Resource instances. 
 * 
 * To persist a Resource, the hosting environment stores the Model instance.
 *
 * @author schuller
 */
public interface Resource extends AutoCloseable {

	/**
	 * called when the resource is first created and
	 * initialized by the hosting environment
	 *
	 * @param initParams
	 * @throws Exception
	 */
	public void initialise(InitParameters initParams) throws Exception;

	public String getUniqueID();

	public String getServiceName();

	public void setHome(Home home);

	public Home getHome();

	public void setKernel(Kernel kernel);

	public Kernel getKernel();

	/**
	 * returns true if the resource was destroyed
	 */
	public boolean isDestroyed();
	
	/**
	 * Perform resource-specific clean up operations.
	 * Specifically, this will be executed:
	 * <ul>
	 * <li>when external clients destroy the resource via some API call</li>
	 * <li>when the lifetime expires and the container destroys the resource</li>
	 * </ul>
	 */
	public void destroy();
	
	/**
	 * the model holds the resource's state
	 */
	public Model getModel();

	public void setModel(Model model);

	/**
	 * Called after the instance was reloaded into memory, all
	 * the fields (model etc) have been set and any access control check 
	 * has been passed
	 */
	public default void activate() {}

	/**
	 * Called when messages are available for this resource which should be
	 * processed. This method will be called by the container typically at
	 * the beginning of a client call, after the resource is properly activated,
	 * has been locked, and before the actual method is invoked.
	 */
	public default void processMessages(PullPoint messageIterator) {}

	/**
	 * Allows to run sanity checks after the server has been restarted, if the {@link Home} implementation
	 * invokes it. This method will be invoked (by the service's {@link Home} object) after the services 
	 * have been deployed, but before the user defined startup tasks are run. 
	 */
	public default void postRestart()throws Exception {}

	/**
	 * Delete the given children. Deletion includes removal from the 
	 * model AND actual removal (destruction) of the resource
	 *  
	 * @param children
	 * @return Collection of child IDs that were found and destroyed
	 */
	public Collection<String> deleteChildren(Collection<String>children);

}
