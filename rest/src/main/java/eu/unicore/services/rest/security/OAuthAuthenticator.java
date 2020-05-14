package eu.unicore.services.rest.security;

import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.message.Message;
import org.apache.http.HttpMessage;
import org.json.JSONObject;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.client.OAuthBearerTokenOutInterceptor;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Authenticate by checking the OAuth Bearer vs an OAuth "userinfo" endpoint.
 * 
 * @author schuller 
 */
public class OAuthAuthenticator extends BaseRemoteAuthenticator<JSONObject> {

	private final static Collection<String> s = Collections.singletonList("Bearer");

	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	protected Object extractCredentials(DefaultClientConfiguration clientCfg, 
			Message message, SecurityTokens tokens) {
		String bearerToken = CXFUtils.getBearerToken(message);
		if(bearerToken == null)return null;

		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("n/a");
		clientCfg.setHttpUser("n/a");
		clientCfg.getExtraSecurityTokens().put(OAuthBearerTokenOutInterceptor.TOKEN_KEY, bearerToken);

		// store token for later use
		tokens.getUserPreferences().put("UC_OAUTH_BEARER_TOKEN", new String[]{bearerToken});

		return bearerToken;
	}

	protected JSONObject performAuth(DefaultClientConfiguration clientCfg) throws Exception {
		String token = String.valueOf(clientCfg.getExtraSecurityTokens().get(OAuthBearerTokenOutInterceptor.TOKEN_KEY));
		IAuthCallback cb = new IAuthCallback() {
			@Override
			public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
				httpMessage.addHeader("Authorization", "Bearer "+token);
			}
		};
		BaseClient bc = new BaseClient(address, clientCfg, cb);
		return bc.getJSON();
	}

	protected void extractAuthInfo(JSONObject auth, SecurityTokens tokens){
		String dn = auth.optString("x500name");
		if(dn != null){
			tokens.setUserName(dn);
			tokens.setConsignorTrusted(true);
		}
	}
	
	public String toString(){
		return "OAuth Bearer Token ["+super.toString()+"]";
	}

	
}
