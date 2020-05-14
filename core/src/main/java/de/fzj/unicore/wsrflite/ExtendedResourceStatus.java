package de.fzj.unicore.wsrflite;

/**
 * A Resource may have a more complex lifecycle and internal stati. This interface allows to query
 * the status and associated status details. 
 * 
 * A resource supporting this interface may be in the following states. 
 * 
 * <ul>
 *  <li>UNDEFINED : the state is not defined </li>
 *  <li>INITIALIZING : the resource is not yet fully ready for use, but some functionality may already be available </li>
 *  <li>READY : the resource is ready for use </li>
 *  <li>DISABLED : the resource has been (temporarily) disabled, invoking some functions may lead to errors</li>
 *  <li>ERROR : the resource cannot be used due to an error condition </li>
 *  <li>SHUTTING_DOWN : the resource is being decommissioned, but some functionality may still be available</li>
 * </ul>
 * 
 * @author schuller
 */
public interface ExtendedResourceStatus {

	/**
	 * Get a detailed human-readable message associated with the current status. This may be <code>null</code> if
	 * there is no particular message is available
	 */
	public String getStatusMessage();
	
	/**
	 * get the current resource status
	 */
	public ResourceStatus getResourceStatus();
	
	/**
	 * Set a detailed human-readable message associated with the current status
	 */
	public void setStatusMessage(String message);
	
	/**
	 * update the current resource status
	 */
	public void setResourceStatus(ResourceStatus status);
	
	/**
	 * for convenience this method checks if the current status is READY, i.e. it returns
	 * <code>true</code> if and only if <code>ResourceStatus.READY==getResourceStatus()</code>
	 */
	public boolean isReady();
	
	/**
	 * Enumerate the different resource stati
	 */
	public static enum ResourceStatus{
		UNDEFINED,
		INITIALIZING,
		READY,
		DISABLED,
		ERROR,
		SHUTTING_DOWN,
	};
}
