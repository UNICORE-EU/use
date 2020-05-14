package de.fzj.unicore.wsrflite.exceptions;

/**
 * thrown when the server cannot find a ws-resource
 * @author schuller
 */
public class ResourceUnknownException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceUnknownException() {
		super();
	}

	public ResourceUnknownException(String message) {
		super(message);
	}

}
