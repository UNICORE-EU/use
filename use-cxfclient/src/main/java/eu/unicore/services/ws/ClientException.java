package eu.unicore.services.ws;

/**
 * thrown on errors related to web service invocations, where the client
 * is the source of the fault. 
 * 
 * @author schuller
 * @since 1.8.11
 */
public class ClientException extends Exception{
	
	private static final long serialVersionUID=1L;

	public ClientException() {
		super();
	}

	public ClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClientException(String message) {
		super(message);
	}

	public ClientException(Throwable cause) {
		super(cause);
	}
	
}
