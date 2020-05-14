package de.fzj.unicore.wsrflite.exceptions;

/**
 * thrown when the server did create a new resource
 * 
 * @author schuller
 */
public class ResourceNotCreatedException extends USEException {

	private static final long serialVersionUID = 1L;

	public ResourceNotCreatedException() {
		super();
	}

	public ResourceNotCreatedException(String message) {
		super(message);
	}

	public ResourceNotCreatedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceNotCreatedException(Throwable cause) {
		super(cause);
	}

}
