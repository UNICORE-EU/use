package eu.unicore.services.rest.security;

import java.util.Collection;

import org.apache.cxf.message.Message;

import eu.unicore.security.SecurityTokens;

/**
 * authenticate user:  use information from security tokens to
 * assign a valid user DN, i.e. after successful AuthN the method
 * {@link SecurityTokens#getEffectiveUserName()} will return the 
 * user DN.
 * 
 * The first successful AuthN will be the effective one
 * 
 * @author schuller
 */
public interface IAuthenticator {

	/**
	 * authenticate: use information from security tokens (and, if required, 
	 * from the request) to assign a valid user DN. 
	 * I.e. if successful, {@link SecurityTokens#getEffectiveUserName()} will
	 * return the user DN
	 * 
	 * @param message - the current {@link Message}
	 * @param tokens - the security tokens
	 * @return <code>true</code> if request contained the authentication material
	 */
	public boolean authenticate(Message message, SecurityTokens tokens);

	/**
	 * get the supported HTTP authentication schemes ("Basic", "Digest", etc) supported by this authenticator
	 */
	public Collection<String> getAuthSchemes();
	
	public default RESTSecurityProperties getSecurityProperties() { 
		return null;
	}
	
}
