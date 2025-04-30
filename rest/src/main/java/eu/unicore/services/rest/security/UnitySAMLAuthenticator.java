package eu.unicore.services.rest.security;

import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.message.Message;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Authenticate to Unity via its SAML interface 
 * using HTTP Basic auth username and password.
 * Assertions are validated using the container's 
 * configured trusted assertion issuers
 *
 * @author schuller 
 */
public class UnitySAMLAuthenticator extends UnityBaseSAMLAuthenticator {

	private final static Collection<String> s = Collections.singletonList("Basic");

	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	@Override
	public String toString(){
		return "Unity with username+password ["+super.toString()+"]";
	}

	@Override
	protected Object extractCredentials(DefaultClientConfiguration clientCfg, Message message, SecurityTokens tokens) {
		HTTPAuthNTokens http = CXFUtils.getHTTPCredentials(message);
		if(http == null)return null;
		String username=http.getUserName();
		String password=http.getPasswd();
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser(username);
		clientCfg.setHttpPassword(password);
		return username+":"+new String(password);
	}

}
