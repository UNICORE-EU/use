package eu.unicore.services;

import java.util.concurrent.TimeUnit;

/**
 * Components connecting to other systems (like a gateway or a registry) can implement this interface
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

	/**
	 * trigger config refresh
	 * @param kernel
	 * @throws Exception
	 */
	default public void reloadConfig(Kernel kernel) throws Exception {}

	/**
	 * OPTIONAL:
	 * in some situations the potentially async status update can be unwanted -
	 * this methods waits for the end of a status update that might in progress.
	 * (The default implementation does nothing)
	 * @param timeout
	 * @param units
	 */
	default public void awaitConnectionStatusRefresh(long timeout, TimeUnit units) {}
}
