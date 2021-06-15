package eu.unicore.services.exceptions;

/**
 * USE error codes. These are in the range 1000-1999, or 0 for an unspecified error code
 * 
 * @author schuller
 */
public interface ErrorCodes {

	/**
	 * unspecified error
	 */
	public static final int ERR_UNSPECIFIED=0;
	
	/**
	 * the service instance limit has been exceeded
	 */
	public static final int ERR_INSTANCE_LIMIT_EXCEEDED=1001;
	
}
