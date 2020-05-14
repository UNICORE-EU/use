package de.fzj.unicore.wsrflite;


/**
 * a Capability provides information about a concrete implementation of
 * some abstract interface
 * 
 * @author schuller
 */
public interface ClientCapability {

	/**
	 * the service interface implemented by the capability
	 */
	public Class<?> getInterface();
	
	/**
	 * the implementation
	 */
	public Class<?> getImplementation();

}
