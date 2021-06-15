package eu.unicore.services.exceptions;

import java.util.Properties;

/**
 * USE level exception that contains an error code, and 
 *
 * @author schuller
 */
public class USEException extends Exception implements ErrorCodes{
	
	private static final long serialVersionUID=1l;
	
	private int errorCode=0;
	
	protected final static Properties messages=new Properties();
	
	static{
		messages.put(ERR_UNSPECIFIED, "No further details are available");
	}

	public USEException() {
	}

	public USEException(String message) {
		super(message);
	}

	public USEException(Throwable cause) {
		super(cause);
	}

	public USEException(String message, Throwable cause) {
		super(message, cause);
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorDescription(){
		return messages.getProperty(String.valueOf(errorCode), "No further details are available");
	}
	
}
