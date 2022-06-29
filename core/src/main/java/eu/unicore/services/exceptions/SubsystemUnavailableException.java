package eu.unicore.services.exceptions;

/**
 * thrown when an external / subsystem cannot be accessed
 * because it is (temporarily) unavalaibe
 * 
 * @author schuller
 */
public class SubsystemUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SubsystemUnavailableException() {
		super();
	}

	public SubsystemUnavailableException(String message) {
		super(message);
	}

	public SubsystemUnavailableException(String message, Throwable cause) {
		super(message,cause);
	}
}
