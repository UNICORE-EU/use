package eu.unicore.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Parent/child relationships between {@link Resource} instances in USE are 
 * managed using this interface
 *  
 * @author schuller
 */
public interface Node {

	/**
	 * the UUID of this node
	 */
	public String getUniqueID();

	/**
	 * get the UID of the parent
	 */
	public String getParentUID();
	
	/**
	 * get the service name of the parent
	 */
	public String getParentServiceName();

	/**
	 * get the children of the resource. Key is the service name
	 */
	public Map<String,List<String>> getChildren();
	
	/**
	 * add a node as a child to this node
	 * @param serviceName - child service name 
	 * @param uid - child ID
	 */
	public void addChild(String serviceName, String uid);

	/**
	 * remove all the children
	 */
	public void removeAllChildren();

	/**
	 * remove a child by unique ID
	 * @param uid
	 * @return <code>true</code> if the child was found and removed, <code>false</code> otherwise
	 */
	public boolean removeChild(String uid);

	/**
	 * remove children by unique IDs
	 * @param uids
	 * @return (non-null) collection of UIDs of the children that were found and removed
	 */
	public Collection<String> removeChildren(Collection<String>uids);
	
	/**
	 * get a (modifiable, never-null) list of all children of a particular type (service name)
	 * 
	 * @param serviceName
	 * @return list of UIDs of the children of the given type
	 */
	public List<String> getChildren(String serviceName);
	
}
