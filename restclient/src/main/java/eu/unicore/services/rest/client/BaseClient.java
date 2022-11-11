package eu.unicore.services.rest.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.SessionIDProvider;
import eu.unicore.util.httpclient.SessionIDProviderImpl;

/**
 * Basic client for working with REST, JSON and all that
 * 
 * An instance of this client is intended to work with a single UNICORE server.
 * It automatically handles security session IDs and user preferences <br/>
 * 
 * NOT THREADSAFE
 * 
 * @author schuller
 */
public class BaseClient {

	protected Logger logger = Log.getLogger(Log.CLIENT, BaseClient.class);
	
	protected final HttpClient client;

	protected StatusLine status;

	protected IAuthCallback authCallback;

	protected SessionIDProvider sessionIDProvider;
	
	protected IClientConfiguration security;

	protected boolean useSessions;
	
	protected String url;
	
	private final Deque<String>urlStack = new ArrayDeque<>();
	
	protected UserPreferences userPreferences = new UserPreferences();
	
	public BaseClient(String url, IClientConfiguration security){
		this(url,security,null);
	}

	public BaseClient(String url, IClientConfiguration security, IAuthCallback authCallback){
		HttpClient client = HttpUtils.createClient(url, security);
		this.client = client;
		this.authCallback = authCallback;
		this.sessionIDProvider = security.getSessionIDProvider();
		if(sessionIDProvider==null){
			sessionIDProvider = new SessionIDProviderImpl();
		}
		this.security = security;
		this.useSessions = security.useSecuritySessions();
		this.url = url;
		setupUserPreferences();
	}

	protected void setupUserPreferences() {
		try {
			Map<String, String[]> userPrefs = security.getRequestedUserAttributes();
			if(userPrefs.get("role")!=null) {
				userPreferences.setRole(userPrefs.get("role")[0]);
			}
			if(userPrefs.get("uid")!=null) {
				userPreferences.setUid(userPrefs.get("uid")[0]);
			}
			if(userPrefs.get("group")!=null) {
				userPreferences.setGroup(userPrefs.get("group")[0]);
			}
			if(userPrefs.get("supplementaryGroups")!=null) {
				userPreferences.setSupplementaryGroups(userPrefs.get("supplementaryGroups"));
			}
		}catch(Exception ex) {
			Log.logException("Cannot configure user preferences.", ex);
		}
	}
	
	/**
	 * set the URL of the resource to access - can be reverted to the previous state using pop()
	 */
	public void pushURL(String url){
		urlStack.push(this.url);
		this.url = url;
	}
	
	/**
	 * revert to the previous URL
	 */
	public void popURL() {
		this.url = urlStack.pop();
	}
	
	/**
	 * set the URL of the resource to access (also clears url "history")
	 */
	public void setURL(String url){
		this.url = url;
		urlStack.clear();
	}
	
	public String getURL(){
		return url;
	}
	
	public UserPreferences getUserPreferences() {
		return userPreferences;
	}
	
	public IAuthCallback getAuthCallback() {
		return authCallback;
	}

	public IClientConfiguration getSecurityConfiguration() {
		return security;
	}

	public void setAuthCallback(IAuthCallback authCallback){
		this.authCallback = authCallback;
	}

	protected void addAuth(HttpMessage message) throws Exception {
		if(authCallback!=null){
			authCallback.addAuthenticationHeaders(message);
		}
	}

	protected void addUserPreferences(HttpMessage message) throws Exception {
		if(userPreferences!=null){
			userPreferences.addUserPreferencesHeader(message);
		}
	}
	
	/**
	 * get the JSON representation of this resource
	 *  
	 * @return {@link JSONObject}
	 * @throws Exception
	 */
	public JSONObject getJSON() throws Exception {
		return getJSON(ContentType.APPLICATION_JSON);
	}

	/**
	 * get the JSON representation of this resource
	 * 
	 * @param accept - the MediaType to use, if <code>null</code>, "application/json" is used
	 * @throws Exception
	 */
	public JSONObject getJSON(ContentType accept) throws Exception {
		ClassicHttpResponse response = get(accept==null ? ContentType.APPLICATION_JSON : accept);
		checkError(response);
		return asJSON(response);
	}
	
	public ClassicHttpResponse get(ContentType accept) throws Exception {
		return get(accept, null);
	}

	/**
	 *
	 * @param accept
	 * @param headers - custom headers (can be null)
	 */
	public ClassicHttpResponse get(ContentType accept, Map<String,String>headers) throws Exception {
		HttpGet get=new HttpGet(url);
		if(accept!=null)get.setHeader("Accept", accept.getMimeType());
		if(headers!=null){
			for(Map.Entry<String, String> header: headers.entrySet()){
				get.setHeader(header.getKey(), header.getValue());
			}
		}
		return execute(get);
	}
	
	/**
	 * put content to this resource, returning the response. The caller must deal with
	 * the response to avoid resource leaks and blocked connections!
	 * 
	 * @param content
	 * @param type
	 * @return HttpResponse
	 * @throws Exception
	 */
	public ClassicHttpResponse put(InputStream content, ContentType type) throws Exception {
		HttpPut put=new HttpPut(url);
		put.setEntity(new InputStreamEntity(content, type));
		ClassicHttpResponse response = execute(put);
		checkError(response);
		return response;
	}
	
	/**
	 * put content to this resource, discarding any response
	 */
	public void putQuietly(InputStream content, ContentType type) throws Exception {
		HttpPut put=new HttpPut(url);
		put.setEntity(new InputStreamEntity(content, type));
		try(ClassicHttpResponse response = execute(put)){
			checkError(response);
			EntityUtils.consumeQuietly(response.getEntity());
		}
	}

	/**
	 * PUT the JSON to this resource
	 * 
	 * @return {@link JSONObject}
	 * @throws Exception
	 */
	public ClassicHttpResponse put(JSONObject content) throws Exception {
		HttpPut put=new HttpPut(url);
		put.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		put.setEntity(new StringEntity(content.toString(), ContentType.APPLICATION_JSON));
		ClassicHttpResponse response = execute(put);
		checkError(response);
		return response;
	}

	/**
	 * PUT the JSON to this resource, discard the response
	 */
	public void putQuietly(JSONObject content) throws Exception {
		try(ClassicHttpResponse response = put(content)){
			checkError(response);
			EntityUtils.consumeQuietly(response.getEntity());
		}
	}

	/**
	 * create a new resource by POSTing the JSON to this resource.
	 * Returns the value of the "Location" header which is 
	 * the URL of the newly created resource.
	 * @return URL of the newly created resource or null if not available
	 */
	public String create(JSONObject content) throws Exception {
		HttpPost post=new HttpPost(url);
		post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		if(content!=null)post.setEntity(new StringEntity(content.toString(), ContentType.APPLICATION_JSON));
		try(ClassicHttpResponse response = execute(post)){
			return response.getFirstHeader("Location").getValue();
		}catch(Exception ex){}
		return null;
	}
	
	/**
	 * post the JSON to this resource and return the response. 
	 * NOTE: the caller is responsible for reading content and closing the response!
	 */
	public ClassicHttpResponse post(JSONObject content) throws Exception {
		HttpPost post=new HttpPost(url);
		post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		if(content!=null)post.setEntity(new StringEntity(content.toString(), ContentType.APPLICATION_JSON));
		return execute(post);
	}

	/**
	 * post content to this resource, discarding the response
	 */
	public void postQuietly(JSONObject content) throws Exception {
		try(ClassicHttpResponse response = post(content)){
			checkError(response);
			EntityUtils.consume(response.getEntity());
		}
	}

	/**
	 * delete this resource
	 */
	public void delete() throws Exception {
		HttpDelete d=new HttpDelete(url);
		try(ClassicHttpResponse response = execute(d)){
			checkError(response);
			EntityUtils.consume(response.getEntity());
		}
	}

	/**
	 * get the named link
	 * 
	 * @param linkName - the name of the link 
	 * @return URL of the linked resource
	 * @throws Exception
	 */
	public String getLink(String linkName) throws Exception {
		return getLink(getJSON(), linkName);
	}
	
	/**
	 * get the named link
	 * 
	 * @param resourceProperties - the JSON representation of the resource
	 * @param linkName - the name of the link 
	 * @return URL of the linked resource
	 * @throws Exception
	 */
	public String getLink(JSONObject resourceProperties, String linkName) throws Exception {
		return resourceProperties.getJSONObject("_links").getJSONObject(linkName).getString("href");
	}

	/**
	 * get the HTTP {@link StatusLine} of the last invocation
	 */
	public StatusLine getLastStatus(){
		return status;
	}

	/**
	 * get the HTTP status code of the last invocation
	 */
	public int getLastHttpStatus(){
		return status!=null? status.getStatusCode() : -1;
	}

	public JSONObject asJSON(ClassicHttpResponse response) throws IOException, JSONException {
		try{
			return new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
		}
		finally{
			close(response);
		}
	}
	
	public String getSessionKey() {
		String key = authCallback!=null? authCallback.getSessionKey() : null;
		String prefsKey = userPreferences!=null? userPreferences.getEncoded() : null;
		if(key!=null && prefsKey!=null && prefsKey.length()>0) {
			key = key + prefsKey;
		}
		return key;
	}
	
	protected ClassicHttpResponse execute(HttpUriRequestBase method) throws Exception {
		ClassicHttpResponse response = null;
		boolean execWithAuth = !useSessions;
		String sessionKey = null;
		
		if(useSessions){
			method.removeHeaders(SecuritySessionUtils.SESSION_ID_HEADER);
			sessionKey = getSessionKey();
			String sessionId = sessionIDProvider.getSessionID(url, sessionKey);
			if(sessionId!=null){
				method.setHeader(SecuritySessionUtils.SESSION_ID_HEADER, sessionId);
				response = client.executeOpen(null, method, HttpClientContext.create());
				status = new StatusLine(response);
				if(432==status.getStatusCode()){
					// session is no longer valid
					execWithAuth = true;
				}
			}
			else{
				execWithAuth = true;
			}
		}

		if(execWithAuth){
			method.removeHeaders(SecuritySessionUtils.SESSION_ID_HEADER);
			response = executeWithAuth(method);			
		}
		
		if(useSessions && sessionKey!=null){
			try{
				Header h = response.getFirstHeader(SecuritySessionUtils.SESSION_ID_HEADER);
				if(h!=null){
					long lifetime = 300*1000;
					Header lt = response.getFirstHeader(SecuritySessionUtils.SESSION_LIFETIME_HEADER);
					if(lt!=null){
						lifetime = Long.valueOf(lt.getValue());
					}
					sessionIDProvider.registerSession(h.getValue(), method.getUri().toString(), 
							lifetime, sessionKey);
				}
			}catch(Exception ex){/*ignored*/}
		}
		return response;
	}

	protected ClassicHttpResponse executeWithAuth(HttpUriRequestBase method) throws Exception {
		addAuth(method);
		addUserPreferences(method);
		ClassicHttpResponse response = client.executeOpen(null, method, HttpClientContext.create());
		status = new StatusLine(response);
		return response;
	}

	public void close(HttpResponse response) {
		if(response instanceof CloseableHttpResponse){
			try{ ((CloseableHttpResponse)response).close(); }
			catch(IOException e) {}
		}
	}

	/**
	 * Check if the response represents an error (i.e., HTTP status >= 400). In case of
	 * an error, error information is extracted an a RESTException is thrown.
	 * 
	 * @param response - HTTP response
	 * @throws RESTException - in case the response represents an error
	 */
	public void checkError(ClassicHttpResponse response) throws RESTException {
		if(response.getCode()>399){
			String message = extractErrorMessage(response);
			close(response);			
			throw new RESTException(response.getCode(), response.getReasonPhrase(), message);
		}
	}

	protected String extractErrorMessage(ClassicHttpResponse response) {
		String errMsg = null;
		try{
			String content = EntityUtils.toString(response.getEntity());
			try{
				JSONObject jO=new JSONObject(content);
				for(String key: Arrays.asList("errorMessage", "message")) {
					errMsg = jO.optString(key, null);
					if(errMsg!=null)break;
				}
				if(errMsg==null)errMsg = jO.toString();
			}catch(Exception ex){
				errMsg = extractHTMLError(content);
			}
		}catch(Exception ex){}
		return errMsg;
	}

	private static final Pattern errorPattern = Pattern.compile("<title>(.*)</title>");
	
	public static String extractHTMLError(String html) {
		Matcher m = errorPattern.matcher(html);
		if(m.find())return m.group(1).trim();
		else return "n/a";
	}

	public SessionIDProvider getSessionIDProvider() {
		return sessionIDProvider;
	}

	public void setSessionIDProvider(SessionIDProvider sessionIDProvider) {
		this.sessionIDProvider = sessionIDProvider;
	}

	public static Map<String,String> asMap(JSONObject o){
		Map<String,String>result=new HashMap<>();
		if(o!=null){
			Iterator<String>i = o.keys();
			while(i.hasNext()){
				String key = i.next();
				try{
					result.put(key, String.valueOf(o.get(key)));
				}catch(JSONException ex){}
			}
		}
		return result;
	}
	
	public static JSONObject asJSON(Map<String,String>map){
		JSONObject o=new JSONObject();
		if(map!=null){
			for(Map.Entry<String, String>entry: map.entrySet()){
				try{
					o.put(entry.getKey(), entry.getValue());
				}catch(JSONException e){}
			}
		}
		return o;
	}
}
