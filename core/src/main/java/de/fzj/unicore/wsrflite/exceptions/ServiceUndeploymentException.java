package de.fzj.unicore.wsrflite.exceptions;

/**
 * Is thrown in case of a failed service deployment attempt
 * 
 * @author j.daivandy@fz-juelich.de
 */
public class ServiceUndeploymentException extends RuntimeException {
	
	private static final long serialVersionUID=1L;
	
	public ServiceUndeploymentException(String msg) {
		super(msg);
	}
	
	public ServiceUndeploymentException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public ServiceUndeploymentException(Throwable cause) {
		super(cause);
	}
}
