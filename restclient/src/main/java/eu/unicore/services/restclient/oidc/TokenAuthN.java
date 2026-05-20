package eu.unicore.services.restclient.oidc;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.security.AuthenticationException;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.utils.UserLogger;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Base class for authenticating using a token.
 *
 * @author schuller
 */
public class TokenAuthN implements IAuthCallback {

	protected String token = null;

	protected String tokenType = "Bearer";

	protected long lastRefresh;

	protected UserLogger log = new UserLogger() {};

	public void setProperties(Properties properties) {
		setToken(properties.getProperty("token"));
		setTokenType(properties.getProperty("token-type", "Bearer"));
	}

	/**
	 * Set the token type - defaults to "Bearer"
	 * @param tokenType
	 */
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public void setToken(String token) {
		if(token!=null && token.startsWith("@")) {
			try{
				token = FileUtils.readFileToString(new File(token.substring(1)), "UTF-8");
			}catch(IOException e) {
				throw new ConfigurationException("Cannot read token from file <"+token+">");
			}
		}
		this.token = token;
	}

	/**
	 * get a fresh token
	 * @throws Exception if token could not be obtained
	 */
	protected void retrieveToken() throws Exception {
		if(token==null)throw new IllegalArgumentException("No token!");
	}

	/**
	 * get a fresh token, if necessary. By default, this does nothing
	 * @throws Exception
	 */
	protected void refreshTokenIfNecessary() throws Exception {
		// NOP
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		try {
			refreshTokenIfNecessary();
			if(token==null) {
				retrieveToken();
			}
		}catch(Exception e) {
			throw new AuthenticationException("Could not obtain an authentication token", e);
		}
		httpMessage.removeHeaders("Authorization");
		httpMessage.setHeader("Authorization", tokenType+" "+token);
	}

	@Override
	public void setLogger(UserLogger log) {
		this.log = log;
	}

}
