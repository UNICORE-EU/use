package eu.unicore.services;

import java.io.Serializable;
import java.util.Set;

/**
 * The state of a {@link Resource}. It contains information that will
 * be persisted to permanent storage.
 * 
 * @author schuller
 */
public interface Model extends Node, Serializable {

	/**
	 * return the unique ID of the resource
	 */
	public String getUniqueID();
	
	/**
	 * set the unique ID
	 * @param id - the ID to set
	 */
	public void setUniqueID(String id);

	/**
	 * get the tags for this resource
	 */
	public Set<String> getTags();
	
	/**
	 * to separate service frontend from resource implementations,
	 * this method returns the name of the frontend class for
	 * the given service type
	 * 
	 * @param serviceType - service type, e.g. usually "cxf" for web services
	 * @return the class name of the frontend. May return "self" to indicate that the
	 * resource class is also to be used as the frontend (for backwards compatibility)
	 * @deprecated will be replaced by declaring the frontend in the service config
	 */
	@Deprecated
	public String getFrontend(String serviceType);
}
