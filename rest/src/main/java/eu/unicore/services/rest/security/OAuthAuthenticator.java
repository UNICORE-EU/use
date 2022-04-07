package eu.unicore.services.rest.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
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
	protected String validationAddress;

	protected boolean useUserInfo = true;

	public void setDnTemplate(String dnTemplate) {
		this.dnTemplate = dnTemplate;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public void setValidationAddress(String validationAddress) {
		this.validationAddress = validationAddress;
	}

	public void setValidate(boolean validate) {
		this.validate = validate;
	}
	
	public void setUseUserInfo(boolean useUserInfo) {
		this.useUserInfo = useUserInfo;
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
		if(useUserInfo) {
			IAuthCallback cb = new IAuthCallback() {
				@Override
				public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
					httpMessage.addHeader("Authorization", "Bearer "+token);
				}
			};
			userData = new BaseClient(address, clientCfg, cb).getJSON();
		}
		return userData;
	}

	protected JSONObject validate(String token, DefaultClientConfiguration clientCfg) throws Exception {
		HttpPost post = new HttpPost(validationAddress);
		List<NameValuePair> postParameters = new ArrayList<>();
	    postParameters.add(new BasicNameValuePair("client_id", clientID));
	    postParameters.add(new BasicNameValuePair("client_secret", clientSecret));
	    postParameters.add(new BasicNameValuePair("token", token));
	    post.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
		HttpClient client = HttpUtils.createClient(validationAddress, clientCfg);
		HttpResponse response = client.execute(post);
		String reply = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		JSONObject j = new JSONObject(reply);
		logger.debug("Validation result: {}", j);
		if(!j.optBoolean("active", false))throw new Exception("Token validation failed");
		return j;
	}

	protected void extractAuthInfo(JSONObject auth, SecurityTokens tokens){
		logger.debug("Got User info {}", auth);
		boolean active = auth.optBoolean("active", true);
		if(!active) {
			return;
		}
		String expanded = RESTUtils.expandTemplate(dnTemplate, auth);
		if(!dnTemplate.equals(expanded)){
			tokens.setUserName(expanded);
			tokens.setConsignorTrusted(true);
		}
	}
	
	public String toString(){
		String adr = useUserInfo? address : validationAddress;
		return "OAuth Bearer Token ["+adr+"]";
	}

	
}
