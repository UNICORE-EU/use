package eu.unicore.services;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import eu.unicore.persist.PersistenceException;
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
	 * get the service name controlled by this Home
	 */
	public String getServiceName();

	/**
	 * Get a wsrf instance for read access (i.e. without aquiring a lock)
	 *  
	 * @param id the ID of the resource
	 * @throws ResourceUnknownException if no such resource exists
	 * @throws ResourceUnavailableException  if other errors occur
	 */
	public Resource get(String id)throws ResourceUnknownException, ResourceUnavailableException;


	/**
	 * update and get a wsrf instance for read access (i.e. without aquiring a long-term lock)
	 *  
	 * @param id the ID of the resource
	 * @throws ResourceUnknownException if no such resource exists
	 * @throws PersistenceException  if database/persistence problems occur
	 */
	public Resource refresh(String id)throws ResourceUnknownException, ResourceUnavailableException;

	/**
	 * Get a wsrf instance for update (i.e. aquire a lock)
	 *  
	 * @param id the ID of the resource
	 * @throws ResourceUnknownException if no such resource exists
	 * @throws ResourceUnavailableException if the resource cannot be locked within the timeout period
	 */
	public Resource getForUpdate(String id)throws ResourceUnknownException, ResourceUnavailableException;

	/**
	 * create a new Resource and persist it
	 * @param init initialization parameters
	 * @return the unique ID of the new instance
	 * @throws ResourceNotCreatedException
	 */
	public String createResource(InitParameters init) throws ResourceNotCreatedException;

	/**
	 * persist the given instance
	 * @param instance
	 * @throws Exception 
	 */
	public void persist(Resource instance) throws Exception;

	/**
	 * Lock the given instance, i.e. upgrade from "read" to "write permission"
	 * 
	 * @param instance
	 * @throws ResourceUnavailableException if the resource cannot be locked within the timeout period
	 */
	public void lock(Resource instance) throws ResourceUnavailableException;

	/**
	 * delete and cleanup the administrative information about a Resource
	 * (NOTE: this will <b>not</b> call {@link Resource#destroy()}
	 * (NOTE 2: locks will be cleaned as well}
	 * 
	 * @param resourceId - the ID of the resource to destroy
	 * @throws Exception
	 */
	public void destroyResource(String resourceId) throws Exception;


	/**
	 * Get the termination time of an WS-Resource
	 * 
	 * @param resourceId
	 * @return Calendar (null if instance does not expire)
	 * @throws ResourceUnknownException
	 * @throws Exception 
	 */
	public Calendar getTerminationTime(String resourceId) throws ResourceUnknownException, Exception;

	/**
	 * Set the termination time of an WS-Resource
	 * 
	 * @param resourceId
	 * @param newTT - new termination time
	 * 
	 */
	public void setTerminationTime(String resourceId, Calendar newTT)
			throws ResourceUnknownException, Exception;

	/**
	 * get the owner of a resource
	 * 
	 * @param resourceID
	 * @return the DN of the owner
	 */
	public String getOwner(String resourceID);

	/**
	 * retrieve the current number of alive instances
	 * @return long
	 * @throws PersistenceException 
	 */
	public long getNumberOfInstances() throws Exception;

	/**
	 * Stop the expiry checker for this service
	 */
	public abstract void stopExpiryCheckNow();

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
	 * Get child resources accessible to the given client.
	 * Note: the PDP is invoked for each resource
	 * 
	 * @param client - client
	 * @since 3.1.0
	 */
	public List<String> getAccessibleResources(Client client) throws Exception;

	/**
	 * Return the resources that are accessible for the given client.
	 * Note: the PDP is invoked for each resource
	 * 
	 * @param ids - a list of IDs for which accessibility should be checked
	 * @param client - client
	 * @since 3.1.0
	 */
	public List<String> getAccessibleResources(Collection<String> ids, Client client) throws Exception;

	public Kernel getKernel();

	public void setKernel(Kernel kernel);

}
