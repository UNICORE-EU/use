package de.fzj.unicore.wsrflite.exceptions;

/**
 * thrown when the server refuses to change the termination time of a resource
 * to the requested value
 * 
 * @author schuller
 */
public class TerminationTimeChangeRejectedException extends USEException {

	private static final long serialVersionUID = 1L;

	public TerminationTimeChangeRejectedException() {
		super();
	}

	public TerminationTimeChangeRejectedException(String message) {
		super(message);
	}

	public TerminationTimeChangeRejectedException(String message, Throwable cause) {
		super(message, cause);
	}

	public TerminationTimeChangeRejectedException(Throwable cause) {
		super(cause);
	}

}
