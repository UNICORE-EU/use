package eu.unicore.services;

/**
 * a Capability provides information about a concrete implementation of
 * some abstract interface
 * 
 * @author schuller
 */
public interface Capability {

	/**
	 * the service interface implemented by the capability
	 */
	public Class<?> getInterface();
	
	/**
	 * the implementation class
	 */
	public Class<?> getImplementation();

	/**
	 * is the capability available 
	 * (i.e. not disabled in configuration, or unavailable due to some issue) 
	 */
	default boolean isAvailable(){
		return true;
	}

	/**
	 * get the name of the capability
	 */
	default String getName(){
		return getClass().getName();
	}
}
