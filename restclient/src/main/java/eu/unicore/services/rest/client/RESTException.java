package eu.unicore.services.rest.client;

public class RESTException extends Exception {
	
	private static final long serialVersionUID=1l;
	
	private final int status;
	
	public RESTException(int status, String httpError, String customErrorMessage) {
		super(customErrorMessage + " [HTTP " + status + " " + (httpError!=null? httpError : "" + "]")+"]");
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return getMessage();
	}

}
