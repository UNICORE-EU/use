package eu.unicore.services;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.persistence.Store;

/**
 * the Home interface offers methods to create, 
 * lookup and destroy Resources.
 * 
 * It also extends {@link Runnable} to allow running service-specific code after server start, for
 * example recovery after service crashes or other downtimes. To use this feature, simply override
 * the {@link #run()} method in your implementation 
 * 
 * @author schuller
 */
public interface Home extends Runnable, KernelInjectable {

	/**
	 * Activate the service using the given name
	 * Called when the container brings up the service
	 * @throws Exception 
	 */
	public void start(String serviceName) throws Exception;

	/**
	 * Shutdown the service, releasing any held resources. Called when the container is shut down.
	 */
	public void shutdown();

	/**
	 * notify the service that the container configuration might have changed.
	 * This is called in the following circumstances:
	 * <ul>
	 *  <li>the service container has been restarted, possibly with a changed configuration
	 *  <li>the container (or at least the services) configuration has changed
	 * </ul> 
	 */
	public void notifyConfigurationRefresh();

	/**
	 * "manually" trigger expiry checks
	 */
	public void runExpiryCheckNow();
	
	/**
	 * get the service name controlled by this Home
	 */
	public String getServiceName();

	/**
	 * Get a Resource for read access (i.e. without acquiring a lock)
	 *  
	 * @param resourceID the ID of the Resource
	 * @throws ResourceUnknownException if no such resource exists
	 * @throws ResourceUnavailableException  if other errors occur
	 */
	public Resource get(String resourceID)throws ResourceUnknownException, ResourceUnavailableException;


	/**
	 * update and get a Resource for read access (i.e. without acquiring a long-term lock)
	 *  
	 * @param resourceID the ID of the resource
	 * @throws ResourceUnknownException if no such resource exists
	 * @throws ResourceUnavailableException if resource cannot be locked
	 * @throws Exception - if database/persistence problems occur
	 */
	public Resource refresh(String resourceID)throws ResourceUnknownException, ResourceUnavailableException, Exception;

	/**
	 * Get a Resource for update (i.e. acquire a lock). Should be used in a try-with-resources clause<br/>
	 * 
	 * <code>
	 * try(Resource r=home.getForUpdate(id)){
	 *   .. do something with the resource ...
	 * }
	 * </code>
	 *  
	 * @param resourceID the ID of the Resource
	 * @throws ResourceUnknownException if no such resource exists
	 * @throws ResourceUnavailableException if the resource cannot be locked within the timeout period
	 */
	public Resource getForUpdate(String resourceID)throws ResourceUnknownException, ResourceUnavailableException;

	/**
	 * create a new Resource and persist it
	 * @param init initialization parameters
	 * @return the unique ID of the new instance
	 * @throws ResourceNotCreatedException
	 */
	public String createResource(InitParameters init) throws ResourceNotCreatedException;

	/**
	 * Called after the Resource was in use by getForUpdate().
	 * Usually there is no need to call this manually. If the Resource
	 * was not destroy()-ed, it will be written to storage. Otherwise,
	 * a clean-up is performed.
	 * 
	 * @param instance
	 * @throws Exception 
	 */
	public void done(Resource instance) throws Exception;

	/**
	 * Get the termination time of a Resource
	 * 
	 * @param resourceId
	 * @return Calendar (null if instance does not expire)
	 * @throws ResourceUnknownException
	 * @throws Exception 
	 */
	public Calendar getTerminationTime(String resourceId) throws ResourceUnknownException, Exception;

	/**
	 * Set the termination time of a Resource
	 * 
	 * @param resourceId
	 * @param newTT - new termination time
	 * 
	 */
	public void setTerminationTime(String resourceId, Calendar newTT)
			throws ResourceUnknownException, Exception;

	/**
	 * get the owner of a Resource
	 * 
	 * @param resourceID
	 * @return the DN of the owner
	 */
	public String getOwner(String resourceID);

	/**
	 * check whether the service is currently shutting down
	 * 
	 * @return <code>true</code> if the service is currently shutting down
	 */
	public boolean isShuttingDown();

	/**
	 * get the {@link Store} instance, if lower level access to the persistence layer is required
	 * @return Store for this service
	 */
	public Store getStore();

	/**
	 * Get the ids of all Resources accessible to the given client.
	 * Note: the PDP is invoked for each resource
	 * 
	 * @param client - client
	 */
	public List<String> getAccessibleResources(Client client) throws Exception;

	/**
	 * Return the Resources that are accessible for the given client.
	 * Note: the PDP is invoked for each resource
	 * 
	 * @param resourceIDs - a list of IDs for which accessibility should be checked
	 * @param client - client
	 */
	public List<String> getAccessibleResources(Collection<String> resourceIDs, Client client) throws Exception;

	//public Kernel getKernel();

}
