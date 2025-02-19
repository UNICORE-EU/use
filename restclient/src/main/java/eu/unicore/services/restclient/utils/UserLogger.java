package eu.unicore.services.restclient.utils;

/**
 * for outputting messages to the user / console
 */
public interface UserLogger {

	/**
	 * output at INFO level
	 * 
	 * @param msg log4j-style formatted message
	 * @param params
	 */
	public default void info(String msg, Object...params) {}

	/**
	 * output at VERBOSE level
	 * 
	 * @param msg log4j-style formatted message
	 * @param params
	 */
	public default void verbose(String msg, Object...params) {}

	/**
	 * output at DEBUG level
	 * 
	 * @param msg log4j-style formatted message
	 * @param params
	 */
	public default void debug(String msg, Object...params) {}

}
