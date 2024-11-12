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

}
