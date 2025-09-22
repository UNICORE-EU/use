package eu.unicore.services.persistence;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;

/**
 * Persistent storage interface
 *  
 * @author schuller
 * @author daivandy
 */
public interface Store {

	/**
	 * initialise the store
	 *
	 * @param kernel - The Kernel instance
	 * @param serviceName - the name of the service that is persisted
	 */
	public void init(Kernel kernel, String serviceName) throws Exception;

	/**
	 * shutdown this store
	 */
	public void shutdown();

	/**
	 * persist the given Resource
	 */
	public void persist(Resource resource) throws Exception;

	/**
	 * Retrieve a list of all unique ids. Usually these will be in order as
	 * ordered by the underlying DB
	 */
	public List<String> getUniqueIDs() throws Exception;

	/**
	 * Retrieve a list of all unique ids which are tagged with the given tags. 
	 * Usually these will be in order as ordered by the underlying DB
	 */
	public List<String> getTaggedResources(String... tags) throws Exception;

	/**
	 * read a Resource from storage
	 *
	 * @param uniqueID - the id of the Resource to read
	 * @return <code>null</code> if instance does not exist
	 */
	public Resource read(String uniqueID) throws Exception;

	/**
	 * get a Resource from storage, acquiring a lock
	 *
	 * @param uniqueID - the id of the Resource to read
	 * @param time - the maximum time to wait when acquiring a lock
	 * @param timeUnit - the time units
	 */
	public Resource getForUpdate(String uniqueID, long time, TimeUnit timeUnit)throws TimeoutException, Exception;

	/**
	 * clear lock for the given Resource
	 * @param resource
	 */
	public void unlock(Resource resource) throws Exception;

	/**
	 * Delete a Resource, and cleanup the persistence. A held lock will be released and deleted.
	 *
	 * @param uniqueID - the id of the resource to delete
	 */
	public void remove(String uniqueID) throws Exception;

	/**
	 * sets termination time for a resource
	 * 
	 * @param uniqueID - the id of the resource
	 * @param c - termination time for the resource. May be <code>null</code> to indicate infinite lifetime
	 */
	public void setTerminationTime(String uniqueID, Calendar c) throws Exception;

	/**	 
	 * gets current termination times for all Resources in this store
	 */
	public Map<String,Calendar> getTerminationTimes()throws Exception;

}
