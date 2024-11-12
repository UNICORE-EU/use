package eu.unicore.services.exceptions;

/**
 * thrown when the server did create a new resource
 * 
 * @author schuller
 */
public class ResourceNotCreatedException extends Exception {

	private static final long serialVersionUID = 1L;

	public ResourceNotCreatedException(String message) {
		this(message,null);
	}

	public ResourceNotCreatedException(String message, Throwable cause) {
		super(message, cause);
	}

}
