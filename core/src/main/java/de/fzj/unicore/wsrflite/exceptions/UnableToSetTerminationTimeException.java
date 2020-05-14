package de.fzj.unicore.wsrflite.exceptions;

/**
 * thrown when the server cannot fulfil a set termination time request
 * @author schuller
 */
public class UnableToSetTerminationTimeException extends USEException {

	private static final long serialVersionUID = 1L;

	public UnableToSetTerminationTimeException() {
		super();
	}

	public UnableToSetTerminationTimeException(String message) {
		super(message);
	}

	public UnableToSetTerminationTimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnableToSetTerminationTimeException(Throwable cause) {
		super(cause);
	}

}
