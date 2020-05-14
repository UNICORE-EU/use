package de.fzj.unicore.wsrflite.exceptions;

/**
 * Is thrown in case of a failed service deployment attempt
 * 
 * @author j.daivandy@fz-juelich.de
 */
public class ServiceDeploymentException extends RuntimeException {
	
	private static final long serialVersionUID=1L;
	
	public ServiceDeploymentException(String msg) {
		super(msg);
	}
	
	public ServiceDeploymentException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public ServiceDeploymentException(Throwable cause) {
		super(cause);
	}
}
