package eu.unicore.services.exceptions;

/**
 * thrown when a ws-resource cannot be accessed, for example 
 * because it is still in use
 * 
 * @author schuller
 */
public class ResourceUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceUnavailableException() {
		super();
	}

	public ResourceUnavailableException(String message) {
		super(message);
	}

	public ResourceUnavailableException(String message, Throwable cause) {
		super(message,cause);
	}
}
