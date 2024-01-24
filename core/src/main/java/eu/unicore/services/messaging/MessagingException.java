package eu.unicore.services.messaging;

public class MessagingException extends Exception {

	private static final long serialVersionUID = 1L;

	public MessagingException(Throwable t){
		super(t);
	}
	
	public MessagingException(String msg){
		super(msg);
	}
	
	public MessagingException(String message, Throwable cause) {
	    super(message, cause);
	}
	
}
