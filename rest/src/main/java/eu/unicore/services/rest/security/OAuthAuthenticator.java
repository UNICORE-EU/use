package eu.unicore.services.rest.security;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.client.OAuthBearerTokenOutInterceptor;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Authenticate by checking the OAuth Bearer vs an OAuth "userinfo" endpoint.
 * 
 * @author schuller 
 */
public class OAuthAuthenticator extends BaseRemoteAuthenticator<JSONObject> {

	private static final Logger logger = Log.getLogger(Log.SECURITY, OAuthAuthenticator.class);

	private final static Collection<String> s = Collections.singletonList("Bearer");

	protected String dnTemplate="UID=%email";

	protected boolean validate = false;
	protected String clientID;
	protected String clientSecret;

	public void setDnTemplate(String dnTemplate) {
		this.dnTemplate = dnTemplate;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public void setValidate(boolean validate) {
		this.validate = validate;
	}

	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	protected Object extractCredentials(DefaultClientConfiguration clientCfg, 
			Message message, SecurityTokens tokens) {
		String bearerToken = CXFUtils.getBearerToken(message);
		if(bearerToken == null)return null;
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("n/a");
		clientCfg.setHttpUser("n/a");
		clientCfg.getExtraSecurityTokens().put(OAuthBearerTokenOutInterceptor.TOKEN_KEY, bearerToken);
		if(!address.toLowerCase().startsWith("https")) {
			clientCfg.setSslEnabled(false);
		}
		// store token for later use
		tokens.getUserPreferences().put("UC_OAUTH_BEARER_TOKEN", new String[]{bearerToken});

		return bearerToken;
	}

	protected JSONObject performAuth(DefaultClientConfiguration clientCfg) throws Exception {
		String token = String.valueOf(clientCfg.getExtraSecurityTokens().get(OAuthBearerTokenOutInterceptor.TOKEN_KEY));
		JSONObject userData = null;
		if(validate) {
			userData = validate(token, clientCfg);
		}
		else {
			IAuthCallback cb = new IAuthCallback() {
				@Override
				public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
					httpMessage.addHeader("Authorization", "Bearer "+token);
				}
			};
			userData = new BaseClient(address, clientCfg, cb).getJSON();
		}
		logger.debug("User data: {}", userData);
		return userData;
	}

	protected JSONObject validate(String token, DefaultClientConfiguration clientCfg) throws Exception {
		HttpPost post = new HttpPost(address);
		List<NameValuePair> postParameters = new ArrayList<>();
	    postParameters.add(new BasicNameValuePair("client_id", clientID));
	    postParameters.add(new BasicNameValuePair("client_secret", clientSecret));
	    postParameters.add(new BasicNameValuePair("token", token));
	    post.setEntity(new UrlEncodedFormEntity(postParameters, Charset.forName("UTF-8")));
		HttpClient client = HttpUtils.createClient(address, clientCfg);
		String reply = client.execute(null, post, HttpClientContext.create(), new BasicHttpClientResponseHandler());
		JSONObject j = new JSONObject(reply);
		if(!j.optBoolean("active", false))throw new Exception("Token validation failed");
		return j;
	}

	protected void extractAuthInfo(JSONObject auth, SecurityTokens tokens){
		boolean active = auth.optBoolean("active", true);
		if(active) {
			String expanded = RESTUtils.expandTemplate(dnTemplate, auth);
			if(!dnTemplate.equals(expanded)){
				tokens.setUserName(expanded);
				tokens.setConsignorTrusted(true);
				tokens.getContext().put(AuthNHandler.USER_AUTHN_METHOD, "OAUTH");
			}
		}
	}

	public String toString(){
		String mode = validate ? "validate" : "userinfo";
		return "OAuth Bearer Token ["+address+ " mode="+mode+"]";
	}

	@Override
	public String getExternalSystemName(){
		return  "OIDC Server "+simpleAddress;
	}
}
