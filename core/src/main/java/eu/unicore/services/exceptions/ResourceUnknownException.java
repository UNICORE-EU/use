package eu.unicore.services.exceptions;

/**
 * thrown when the server cannot find a Resource
 * @author schuller
 */
public class ResourceUnknownException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceUnknownException() {
		this(null);
	}

	public ResourceUnknownException(String message) {
		super(message);
	}

}