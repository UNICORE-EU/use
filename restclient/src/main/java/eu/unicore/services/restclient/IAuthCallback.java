package eu.unicore.services.restclient;

import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.services.restclient.utils.UserLogger;

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
	 * set an external logger to log to - generally only useful for the more
	 * complex authentication options like SAML or OAuth
	 *
	 * @param log
	 */
	public default void setLogger(UserLogger log) {}

}