package eu.unicore.services.restclient.utils;

import org.apache.logging.log4j.message.FormattedMessage;

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
	public default void info(String msg, Object...params) {
		System.out.println(new FormattedMessage(msg, params));
	}

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

	/**
	 * output an ERROR
	 *
	 * @param t
	 * @param msg log4j-style formatted message
	 * @param params
	 */
	public default void error(Throwable t, String msg, Object...params) {}

}