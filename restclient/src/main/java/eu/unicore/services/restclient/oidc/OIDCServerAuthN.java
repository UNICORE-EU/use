package eu.unicore.services.restclient.oidc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.StatusLine;
import org.json.JSONObject;

import eu.unicore.security.wsutil.client.authn.FilePermHelper;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.oidc.OIDCProperties.AuthMode;
import eu.unicore.services.restclient.utils.ConsoleLogger;
import eu.unicore.services.restclient.utils.UserLogger;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Gets a Bearer token from an OIDC server.
 *
 * @author schuller
 */
public class OIDCServerAuthN implements IAuthCallback {

	private final OIDCProperties properties;
	private final IClientConfiguration clientConfig;
	String token;
	String refreshToken;
	long lastRefresh;
	private UserLogger log = new ConsoleLogger();

	public OIDCServerAuthN(OIDCProperties properties, IClientConfiguration clientConfig) 
	{
		this.properties = properties;
		this.clientConfig = clientConfig;
		loadRefreshToken();
		this.log = new UserLogger(){};
	}

	@Override
	public void setLogger(UserLogger log) {
		this.log = log;
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		if(refreshToken!=null) {
			refreshTokenIfNecessary();
		}
		if(token==null) {
			retrieveToken();
		}
		httpMessage.setHeader("Authorization", "Bearer "+token);
	}

	@Override
	public String getType() {
		return "OIDC-SERVER";
	}

	public String getSessionKey() {
		return properties.getUsername()+"@"+properties.getTokenEndpoint();
	}

	protected void loadRefreshToken() {
		if(!properties.isStoreRefreshTokens())return;
		File tokenFile =  new File(properties.getRefreshTokensFilename());
		try {
			if(tokenFile.exists()) {
				String tokenEndpoint = properties.getTokenEndpoint();
				JSONObject tokens = new JSONObject(FileUtils.readFileToString(tokenFile, "UTF-8"));
				JSONObject token = tokens.optJSONObject(tokenEndpoint);
				refreshToken = token.getString("refresh_token");
			}
		} catch (Exception ex) {
			log.verbose("Cannot load refresh token from <{}>", tokenFile);
		}
	}

	protected void storeRefreshToken() {
		if(refreshToken==null || !properties.isStoreRefreshTokens())return;
		File tokenFile =  new File(properties.getRefreshTokensFilename());
		String tokenEndpoint = properties.getTokenEndpoint();
		JSONObject tokens = new JSONObject();
		JSONObject token = new JSONObject();
		try (FileWriter writer=new FileWriter(tokenFile)){
			token.put("refresh_token", refreshToken);
			tokens.put(tokenEndpoint, token);
			tokens.write(writer);
			FilePermHelper.set0600(tokenFile);
		}catch(Exception e) {
			log.verbose("Cannot store refresh token to <{}>", tokenFile);
		}
	}

	protected void refreshTokenIfNecessary() throws Exception {
		long instant = System.currentTimeMillis() / 1000;
		if(instant < lastRefresh + properties.getRefreshInterval()){
			return;
		}
		lastRefresh = instant;
		log.verbose("Refreshing token from <{}>", properties.getTokenEndpoint());
		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("grant_type", "refresh_token"));
		params.add(new BasicNameValuePair("refresh_token", refreshToken));
		try {
			handleReply(executeCall(params));
		}catch(Exception e) {
			log.verbose("Error refreshing: {}", Log.createFaultMessage("",e));
			token = null;
		}
	}

	protected void retrieveToken() throws Exception {
		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("grant_type", properties.getGrantType()));
		String otp = properties.getOTP();
		if(otp!=null && !otp.isEmpty()) {
			params.add(new BasicNameValuePair(properties.getOTPParamName(), otp));
		}
		params.add(new BasicNameValuePair("username", properties.getUsername()));
		params.add(new BasicNameValuePair("password", properties.getPassword()));
		String scope = properties.getScope();
		if(scope!=null && !scope.isEmpty()) {
			params.add(new BasicNameValuePair("scope", properties.getScope()));
		}
		handleReply(executeCall(params));
		log.verbose("Retrieved new token from <{}>", properties.getTokenEndpoint());
	}

	private void handleReply(JSONObject reply) throws IOException {
		token = reply.optString("access_token", null);
		refreshToken = reply.optString("refresh_token", null);
		lastRefresh = System.currentTimeMillis() / 1000;
		storeRefreshToken();
	}

	private JSONObject executeCall(List<BasicNameValuePair> params) throws Exception {
		String url = properties.getTokenEndpoint();
		HttpPost post = new HttpPost(url);
		String clientID = properties.getClientID();
		String clientSecret = properties.getClientSecret();
		if(AuthMode.BASIC.equals(properties.getAuthMode())){
			post.addHeader("Authorization", 
					"Basic "+new String(Base64.encodeBase64((clientID+":"+clientSecret).getBytes())));
		}
		else {
			params.add(new BasicNameValuePair("client_id", clientID));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
		post.setEntity(entity);
		HttpClient client = HttpUtils.createClient(url, clientConfig);
		try(ClassicHttpResponse response = client.executeOpen(null, post, HttpClientContext.create())){
			String body = "";
			try{
				body = EntityUtils.toString(response.getEntity());
			}catch(Exception ex){};
			if(response.getCode()!=200){
				throw new Exception("Error <"+new StatusLine(response)+"> from OIDC server: "+body);
			}
			return new JSONObject(body);
		}
	}
}
