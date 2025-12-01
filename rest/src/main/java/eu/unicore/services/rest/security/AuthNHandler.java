package eu.unicore.services.rest.security;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.json.JSONObject;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.SecurityException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.SecuritySession;
import eu.unicore.security.wsutil.SecuritySessionStore;
import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.impl.PostInvokeHandler;
import eu.unicore.services.rest.jwt.JWTHelper;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.BasicAttributeHolder;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.util.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

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

	// key for storing the authentication method in the security tokens
	public final static String USER_AUTHN_METHOD = "User-Authentication-Method";
	// key for storing the "renewable" property of a ETD token in the security tokens
	public final static String ETD_RENEWABLE = "ETD-Token-Renewable";

	private final SecuritySessionStore sessionStore;

	private final boolean useSessions;

	private final JWTHelper jwt;

	private String serverDN;

	public AuthNHandler(Kernel kernel, SecuritySessionStore sessionStore){
		IContainerSecurityConfiguration secConfig = kernel.getContainerSecurityConfiguration();
		this.kernel = kernel;
		this.useSessions = secConfig.isSessionsEnabled();
		this.sessionStore = sessionStore;
		JWTServerProperties p = new JWTServerProperties(kernel.getContainerProperties().getRawProperties());
		PubkeyCache cache = PubkeyCache.get(kernel);
		try{
			this.serverDN = secConfig.getCredential().getSubjectName();
			cache.update(this.serverDN, secConfig.getCredential().getCertificate().getPublicKey());
		}catch(Exception ex){}
		this.jwt = new JWTHelper(p, kernel.getContainerSecurityConfiguration(), cache);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ThreadContext.clearAll();
		Message message = PhaseInterceptorChain.getCurrentMessage();
		try{
			Response res = doHandle(message);
			if(res!=null){
				// response will be picked up by the JAXRSInvoker
				message.getExchange().put(Response.class,res);
			}
		}
		catch(SecurityException ex){
			logger.debug(()->Log.createFaultMessage("User authentication failed", ex));
			throw new WebApplicationException(ex, HttpStatus.SC_FORBIDDEN);
		}
	}

	private Response doHandle(Message message){
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
			logger.debug("Re-using session {} for <{}>", sessionID, session.getUserKey());
		}
		AuthZAttributeStore.setTokens(token);
		if(response == null){
			handleUserPreferences(message, token);
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
	 * @return null if client is authenticated, a 'forbidden' response otherwise 
	 */
	private Response process(Message message, SecurityTokens token) {
		token.setClientIP(establishClientIP(message));
		processDelegation(message, token);
		if(token.getConsignorName()!=null && token.isConsignorTrusted()){
			// valid delegation - continue with the request
			return null;
		}
		// no delegation - invoke direct authentication chain
		return processNoDelegation(message, token);
	}

	private void processDelegation(Message message, SecurityTokens tokens){
		String bearer = CXFUtils.getBearerToken(message);
		if(bearer != null){
			try{
				validateJWT(bearer, tokens);
			}catch(Exception ex){
				throw new WebApplicationException(ex, 403);
			}
		}
	}

	/**
	 * check syntax and validate.
	 *
	 * no-op if the token is not a well-formed JWT token,
	 * or if it is not a delegation token
	 */
	private void validateJWT(String bearerToken, SecurityTokens tokens) throws Exception {
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
		boolean isDelegationToken = etd && subject!=null && issuer!=null && subject!=issuer;
		if(isDelegationToken){
			jwt.verifyJWTToken(bearerToken, serverDN);
			tokens.setUserName(subject);
			tokens.setConsignorTrusted(true);
			tokens.setConsignorName(issuer);
			tokens.getContext().put(USER_AUTHN_METHOD, "ETD");
			Boolean renewable = Boolean.parseBoolean(payload.optString("renewable", "false"));
			tokens.getContext().put(ETD_RENEWABLE, renewable);
			logger.debug("Trust delegated authentication as <{}> via JWT issued by <{}>", subject, issuer);
			// process delegated attributes - only if the token came from this server
			BasicAttributeHolder bah = assignAttributes(payload);
			if(bah!=null && serverDN!=null && X500NameUtils.equal(issuer, serverDN)) {
				tokens.getContext().put(AuthAttributesCollector.ATTRIBUTES, bah);
				logger.debug("Attributes from ETD token: {}", bah);
			}
		}
	}

	private Response processNoDelegation(Message message, SecurityTokens token){
		IAuthenticator auth = kernel.getAttribute(IAuthenticator.class);
		boolean haveCredentials  = auth.authenticate(message, token);
		if(!haveCredentials
				&& auth.getSecurityProperties().forbidNoCreds()
				&& kernel.getContainerSecurityConfiguration().isAccessControlEnabled()) {
			throw new AuthenticationException("Authentication failed - no credentials.");
		}
		if(haveCredentials && token.getEffectiveUserName()==null) {
			throw new AuthenticationException("Authentication failed - credentials could not be verified.");
		}
		// OK - continue request processing
		return null;
	}

	private String establishClientIP(Message message){
		String clientIP = CXFUtils.getServletRequest(message).getHeader(CONSIGNOR_IP_HEADER);
		if(clientIP == null) clientIP = CXFUtils.getClientIP(message);
		return clientIP;
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
	private void handleUserPreferences(Message message, SecurityTokens tokens){
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
			preferences.put(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, value.split("\\+"));
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

	private BasicAttributeHolder assignAttributes(JSONObject etdPayload) {
		BasicAttributeHolder bah = new BasicAttributeHolder();
		if(etdPayload.optString("uid", null)!=null) {
			bah.uid = etdPayload.getString("uid");
			bah.setRole("user");
		}
		return bah;
	}
}
