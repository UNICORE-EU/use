package eu.unicore.services;

import java.util.Collection;
import java.util.Collections;

/**
 * All implementations of this interface are run at startup of the container. 
 * The order is determined by the name and getAfter and getBefore methods.
 * @author K. Benedyczak
 */
public interface StartupTask extends Runnable {

	/**
	 * @return name (id) of the startup task - defaults to the class name
	 */
	public default String getName() {
		return getClass().getName();
	}

	/**
	 * @return names of tasks which should be invoked before this one.
	 */
	public default Collection<String> getAfter() {
		return Collections.emptySet();
	}

	/**
	 * @return names of tasks which should be invoked after this one.
	 */
	public default Collection<String> getBefore() {
		return Collections.emptySet();
	}

}
