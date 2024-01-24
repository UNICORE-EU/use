package eu.unicore.services.security.util;

import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;

/**
 * Allows dealing with additional attributes which are to be preserved in the {@link Client} object.
 * 
 * @see eu.unicore.services.security.SecurityManager#addCallback(AttributeHandlingCallback)
 * @author schuller
 */
public interface AttributeHandlingCallback {
	
	/**
	 * retrieve a map of attributes for adding to the client attributes 
	 * 
	 * @param tokens
	 */
	public Map<String,String> extractAttributes(SecurityTokens tokens);
	
	
}
