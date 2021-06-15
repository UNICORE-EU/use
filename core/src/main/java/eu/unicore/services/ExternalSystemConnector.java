package eu.unicore.services;

/**
 * components connecting to other systems (like a gateway or a registry) can implement this interface
 * and register with the Kernel
 * 
 * @author schuller
 */
public interface ExternalSystemConnector {

	public enum Status {
		
		OK, DOWN, UNKNOWN, NOT_APPLICABLE 
		
	};
	
	/**
	 * get a human readable connection status
	 */
	public String getConnectionStatusMessage();
	
	/**
	 * get the status
	 */
	public Status getConnectionStatus();
	
	/**
	 * simple name of the external system, e.g. "TSI"
	 */
	public String getExternalSystemName();
	
}
