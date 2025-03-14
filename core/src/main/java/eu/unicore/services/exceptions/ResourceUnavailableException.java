package eu.unicore.services.exceptions;

/**
 * thrown when a Resource cannot be accessed, for example 
 * because it is still in use
 * 
 * @author schuller
 */
public class ResourceUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceUnavailableException(String message) {
		this(message, null);
	}

	public ResourceUnavailableException(String message, Throwable cause) {
		super(message,cause);
	}
}
