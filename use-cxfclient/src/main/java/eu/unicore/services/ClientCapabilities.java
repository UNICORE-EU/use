package eu.unicore.services;

/**
 * the ClientCapabilities interface is used to automatically load extensions
 * using the Java service provider interface approach.<br>
 * 
 * a ClientCapabilities implementation provides a wrapper around a set of {@link ClientCapability} instances 
 * 
 * @author schuller
 */
public interface ClientCapabilities {

	/**
	 * returns the client capabilities
	 */
	public ClientCapability[] getClientCapabilities();
	
}
