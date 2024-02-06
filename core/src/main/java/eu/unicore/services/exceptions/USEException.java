package eu.unicore.services.exceptions;

/**
 * USE level exception that contains an error code, and 
 *
 * @author schuller
 */
public class USEException extends Exception {
	
	private static final long serialVersionUID=1l;

	public USEException() {
	}

	public USEException(String message) {
		super(message);
	}

	public USEException(Throwable cause) {
		super(cause);
	}

	public USEException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
