package eu.unicore.services.exceptions;

/**
 * thrown when an external / subsystem cannot be accessed
 * because it is (temporarily) unavailable
 *
 * @author schuller
 */
public class SubsystemUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SubsystemUnavailableException(String message) {
		this(message,null);
	}

	public SubsystemUnavailableException(String message, Throwable cause) {
		super(message,cause);
	}
}
