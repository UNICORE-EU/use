package eu.unicore.services.rest.security;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.SecuritySession;
import eu.unicore.security.wsutil.SecuritySessionStore;
import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.impl.PostInvokeHandler;
import eu.unicore.services.rest.jwt.JWTHelper;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.util.Log;

/**
 * AuthN handler for REST services.
 * 
 * Extracts credentials / AuthN data from the incoming message, and
 * creates the {@link SecurityTokens}. 
 * 
 * Handles trust delegation via JWT Bearer token.
 * 
 * Handles security sessions.
 * 
 * @author schuller
 */
public class AuthNHandler implements ContainerRequestFilter {

	private static final Logger logger = Log.getLogger(Log.SECURITY, AuthNHandler.class);

	private final Kernel kernel;

	// special header used by the gateway for forwarding the client's IP address to the VSite
	public final static String CONSIGNOR_IP_HEADER = "X-UNICORE-Consignor-IP";

	// special header used by the gateway for forwarding the GW URL (as sent by the client) to the VSite
	public final static String GW_EXTERNAL_URL = "X-UNICORE-Gateway";

	// special header used by the client for sending preferred xlogin, primary group and role
	public final static String USER_PREFERENCES_HEADER = "X-UNICORE-User-Preferences";

	private final SecuritySessionStore sessionStore;

	private final boolean useSessions;

	private final JWTHelper jwt;
	
	public AuthNHandler(Kernel kernel, SecuritySessionStore sessionStore){
		this.kernel = kernel;
		useSessions = kernel.getContainerSecurityConfiguration().isSessionsEnabled();
		this.sessionStore = sessionStore;

		JWTServerProperties p = new JWTServerProperties(kernel.getContainerProperties().getRawProperties());
		PubkeyCache cache = PubkeyCache.get(kernel);
		IContainerSecurityConfiguration secConfig = kernel.getContainerSecurityConfiguration();
		try{
			cache.update(secConfig.getCredential().getSubjectName(), 
					secConfig.getCredential().getCertificate().getPublicKey());
		}catch(Exception ex){}
		jwt = new JWTHelper(p, kernel.getContainerSecurityConfiguration(), cache);
	}


	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Message message = PhaseInterceptorChain.getCurrentMessage();
		Response res = handleRequest(message);
		if(res!=null){
			// response will be picked up by the JAXRSInvoker
			message.getExchange().put(Response.class,res);
		}
	}


	public Response handleRequest(Message message) {
		try{
			return doHandle(message);
		}
		catch(eu.unicore.security.SecurityException ex){
			Log.logException("User authentication/authorisation failed", ex, logger);
			throw new WebApplicationException(ex, 403);
		}
	}

	protected Response doHandle(Message message){

		Response response = null;
		SecuritySession session = null;
		SecurityTokens token;

		String sessionID = getSecuritySessionID(message);
		if (sessionID==null) {
			token = new SecurityTokens();
			response = process(message, token);
			if(useSessions){
				session = createSession(token);
			}
		} else {
			// re-using session
			session = getSession(message, sessionID);
			token = session.getTokens();
			token.getContext().put(SecuritySessionUtils.REUSED_MARKER_KEY, Boolean.TRUE);
			if(logger.isDebugEnabled()){
				logger.debug("Re-using session "+sessionID+" for <"+session.getUserKey()+">");
			}
		}
		if(response == null){
			handleUserPreferences(message, token);
			Client c = createClient(token);
			if(logger.isDebugEnabled()){
				logger.debug("Authenticated client: "+c);
			}
			AuthZAttributeStore.setClient(c);
			// make sure session info goes to the client
			PostInvokeHandler.setSession(session);
		}
		return response;
	}

	/**
	 * perform authentication
	 * 
	 * @param message
	 * @param token
	 * @return null if client is authenticated, a 401 response otherwise 
	 */
	private Response process(Message message, SecurityTokens token) {
		token.setClientIP(establishClientIP(message));
		processDelegation(message, token);
		if(token.isTrustDelegationValidated()){
			// valid delegation - continue with the request
			return null;
		}
		// no delegation - invoke direct authentication chain
		return processNoDelegation(message, token);
	}
	
	protected void processDelegation(Message message, SecurityTokens tokens){
		String bearer = CXFUtils.getBearerToken(message);
		if(bearer != null){
			try{
				validateJWT(bearer, tokens);
			}catch(Exception ex){
				Log.logException("JWT trust delegation not accepted", ex, logger);
				throw new WebApplicationException(ex, 403);
			}
		}
	}
	

	/**
	 * check syntax and validate.
	 * 
	 * no-op if the token is not a well-formed JWT token
	 */
	protected void validateJWT(String bearerToken, SecurityTokens tokens) throws Exception {
		JSONObject payload;
		try{
			// syntax check first
			payload = JWTUtils.getPayload(bearerToken);
		}catch(Exception jtd){
			return;
		}
		String subject = payload.optString("sub", null);
		String issuer = payload.optString("iss", null);
		boolean etd = Boolean.parseBoolean(payload.optString("etd", null));
		boolean isDelegationToken = etd && 
				subject!=null && issuer!=null && subject!=issuer;
		if(isDelegationToken){
			// TD mode
			jwt.verifyJWTToken(bearerToken);
			tokens.setUserName(subject);
			tokens.setConsignorTrusted(true);
			tokens.setConsignorName(issuer);
			if(logger.isDebugEnabled()){
				logger.debug("Trust delegated authentication as <"+subject+"> via JWT issued by <"+issuer+">");
			}
		}
	}
	
	protected Response processNoDelegation(Message message, SecurityTokens token){
		IAuthenticator auth = kernel.getAttribute(RESTSecurityProperties.class).getAuthenticator();
		boolean haveAuth  = auth.authenticate(message, token);
		if(!haveAuth
				&& kernel.getAttribute(RESTSecurityProperties.class).forbidNoCreds()
				&& kernel.getContainerSecurityConfiguration().isAccessControlEnabled()
				) {
			//this is mostly useful to force browsers to ask the user for credentials
			String realm = kernel.getContainerProperties().getContainerURL();
			ResponseBuilder res = Response.status(401);
			for(String scheme: auth.getAuthSchemes()){
				res.header("WWW-Authenticate", scheme+" realm="+realm);
			}
			return res.build();
		}
		// continue request processing
		return null;
	}

	protected String establishClientIP(Message message){
		String clientIP = CXFUtils.getServletRequest(message).getHeader(CONSIGNOR_IP_HEADER);
		if(clientIP == null) clientIP = CXFUtils.getClientIP(message);
		return clientIP;
	}

	public Client createClient(SecurityTokens tokens) {
		if(logger.isDebugEnabled()){
			logger.debug("Authenticated user: "+tokens.getEffectiveUserName());
		}
		Client client =  kernel.getSecurityManager().createClientWithAttributes(tokens);
		if(client!=null && client.getSecurityTokens()!=null){
			kernel.getSecurityManager().collectDynamicAttributes(client);
		}
		return client;
	}

	/**
	 * Extract user preferences from X-UNICORE-User-Preferences header(s)
	 * 
	 * Attribute names are either as used in UCC
	 * 
	 * vo:val|role:val|uid:val|pgid:val|supgids:val1,val2,...|useOSgids:true|false
	 * 
	 * or the long ones from {@link IAttributeSource}
	 *  
	 */
	protected void handleUserPreferences(Message message, SecurityTokens tokens){
		Enumeration<String>headers = CXFUtils.getServletRequest(message).getHeaders(USER_PREFERENCES_HEADER);
		Map<String, String[]> preferences = tokens.getUserPreferences();
		while(headers.hasMoreElements()){
			String header = headers.nextElement();
			// might have multiple preferences in one header
			for(String value: header.split(",")){
				String[]tok = value.split(":");
				if(tok.length!=2)throw new AuthorisationException("Invalid format for user preference: "+header);
				handlePreference(tok[0], tok[1], preferences);
			}
		}
	}
	
	private void handlePreference(String key, String value, Map<String, String[]> preferences){
		if(IAttributeSource.ATTRIBUTE_XLOGIN.equalsIgnoreCase(key)
				|| "uid".equalsIgnoreCase(key)){
			preferences.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{value});
			return;
		}
		if(IAttributeSource.ATTRIBUTE_ROLE.equalsIgnoreCase(key)){
			preferences.put(IAttributeSource.ATTRIBUTE_ROLE, new String[]{value});
			return;
		}
		if(IAttributeSource.ATTRIBUTE_GROUP.equalsIgnoreCase(key)
				|| "pgid".equalsIgnoreCase(key)){
			preferences.put(IAttributeSource.ATTRIBUTE_GROUP, new String[]{value});
			return;
		}
		if(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS.equalsIgnoreCase(key)
				|| "supgids".equalsIgnoreCase(key)){
			preferences.put(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, value.split("+"));
			return;
		}
		if(IAttributeSource.ATTRIBUTE_SELECTED_VO.equalsIgnoreCase(key)
				|| "vo".equalsIgnoreCase(key)){
			preferences.put(IAttributeSource.ATTRIBUTE_SELECTED_VO, new String[]{value});
			return;
		}
	}

	private String getSecuritySessionID(Message message){
		if(!useSessions)return null;
		return CXFUtils.getServletRequest(message).getHeader(SecuritySessionUtils.SESSION_ID_HEADER);
	}

	private SecuritySession getSession(Message message, String sessionID){
		SecuritySession session = null;
		session=sessionStore.getSession(sessionID);
		if (session==null || session.isExpired()){
			// got a session ID from the client, but no session: fault
			throw new WebApplicationException(432);
		}
		return session;
	}

	private SecuritySession createSession(SecurityTokens securityTokens){
		SecuritySession session = null;
		// lifetime in milliseconds
		long lt = kernel.getContainerSecurityConfiguration().getSessionLifetime()*1000;
		String sessionID=UUID.randomUUID().toString();
		securityTokens.getContext().put(SecuritySessionUtils.SESSION_ID_KEY, sessionID);
		session = new SecuritySession(sessionID, securityTokens, lt);
		sessionStore.storeSession(session, securityTokens);
		return session;
	}
}
