package eu.unicore.services.rest.client;

import org.apache.http.HttpMessage;

/**
 * callback to add authentication headers to REST calls
 * 
 * @author schuller
 */
public interface IAuthCallback {

	/**
	 * add authentication headers
	 * 
	 * @param httpMessage - the outgoing message
	 * @throws Exception - if authentication headers are required for the call but cannot be created
	 */
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception;
	
	/**
	 * uniquely identify the user we are authenticating - must be suitable for use as 
	 * a session key.
	 */
	public default String getSessionKey() {
		return toString();
	}

	/**
	 * identifier of the concrete auth implementation
	 */
	public default String getType() {
		return getClass().getSimpleName();
	}

}
