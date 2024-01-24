package eu.unicore.services.impl;

import eu.unicore.persist.PersistenceException;
import eu.unicore.services.Home;
import eu.unicore.services.exceptions.ResourceUnknownException;

/**
 * instances of this class are used for periodical checks on all the
 * resources existing in a specific {@link DefaultHome} 
 * 
 * @author schuller
 * @author demuth
 */
public interface InstanceChecker {

	/**
	 * check condition
	 * @param uniqueID - a resource id
	 * @throws ResourceUnknownException - the the given id does not correspond to a Resource
	 * @throws PersistenceException  - on database/persistence problems
	 */
	public boolean check(Home home, String uniqueID) throws Exception;

	/**
	 * perform processing on the instance (in case check() hits)
	 * @param uniqueID - a resource id
	 * @return returns <code>true</code> if instance is still valid. If it is 
	 * invalid, <code>false</code> is returned, and the instance will be removed from further checks
	 */
	public boolean process(Home home, String uniqueID) throws ResourceUnknownException;


}