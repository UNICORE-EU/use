package eu.unicore.services.exceptions;

/**
 * thrown when an insert,update,delete operation on a resource property 
 * results in an invalid resource property document
 * 
 * @author schuller
 */
public class InvalidModificationException extends USEException {

	private static final long serialVersionUID = 1L;

	public InvalidModificationException() {
		super();
	}

	public InvalidModificationException(String message) {
		super(message);
	}

	public InvalidModificationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidModificationException(Throwable cause) {
		super(cause);
	}

}
