package eu.unicore.services.exceptions;

/**
 * thrown when an insert,update,delete operation on a resource property 
 * is not accepted by the server
 * @author schuller
 */
public class InvalidModificationException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidModificationException(String message) {
		this(message, null);
	}

	public InvalidModificationException(String message, Throwable cause) {
		super(message, cause);
	}

}
