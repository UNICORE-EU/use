package eu.unicore.services.rest.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.persist.Persist;
import eu.unicore.persist.util.UUID;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.Client.Type;
import eu.unicore.security.SecurityException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.SecuritySession;
import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.persistence.InstanceInfoBean;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.restclient.UserPreferences;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.BasicAttributeHolder;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.Log;
import io.imunity.tanl.x509.X509Credential;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * API token management
 * 
 * @author schuller
 */
public class Tokens extends RESTRendererBase {

	private static final Logger logger = Log.getLogger(Log.SERVICES, Tokens.class);

	private final Kernel kernel;

	public Tokens(Kernel kernel, String baseURL){
		this.kernel = kernel;
		this.baseURL = baseURL;
	}

	@GET
	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	public Response createNewToken(@QueryParam("lifetime")String lifetimeParam, 
			@QueryParam("renewable")String renewable) throws Exception {
		try {
			if(isTokenAuth()) {
				if(!(Boolean)AuthZAttributeStore.getTokens().getContext().get(AuthNHandler.RENEWABLE_API_TOKEN)) {
					throw new AuthenticationException("Cannot create token when authenticating with a non-renewable token!");
				}
			}
			JWTServerProperties jwtProps = new JWTServerProperties(kernel.getContainerProperties().getRawProperties());
			String user = AuthZAttributeStore.getClient().getDistinguishedName();
			X509Credential issuerCred =  kernel.getContainerSecurityConfiguration().getCredential();
			long lifetime = lifetimeParam!=null? Long.valueOf(lifetimeParam): jwtProps.getTokenValidity();
			// make sure it's not longer than the remaining credential lifetime
			Date notAfter = issuerCred.getCertificate().getNotAfter();
			long remainingCredentialLifetime = Math.max(0, notAfter.getTime() - System.currentTimeMillis());
			// if user requested a longer lifetime than is possible, we should fault
			if(lifetimeParam!=null && lifetime>remainingCredentialLifetime) {
				return createErrorResponse(HttpStatus.SC_BAD_REQUEST,
						"Requested token lifetime is longer than the remaining server certificate validity.");
			}
			lifetime = Math.min(lifetime, remainingCredentialLifetime);
			Map<String,String> claims = new HashMap<>();
			claims.put("apitoken", "true");
			try {
				if(Type.ANONYMOUS!=AuthZAttributeStore.getClient().getType()) {
					SecurityTokens tokens = AuthZAttributeStore.getTokens();
					BasicAttributeHolder attr = (BasicAttributeHolder)tokens.getContext().get(AuthAttributesCollector.ATTRIBUTES);
					if(attr!=null && attr.uid!=null) {
						claims.put("uid", attr.uid);
					}
					Map<String,String[]> prefs = tokens.getUserPreferences();
					UserPreferences up = new UserPreferences();
					up.parse(prefs);
					String enc = up.getEncoded();
					if(enc!=null && enc.length()>0) {
						claims.put("preferences", enc);
					}
				}
			}catch(Exception e) {}
			if(Boolean.parseBoolean(renewable)) {
				claims.put("renewable", "true");
			}
			String token = issueToken(kernel, user, remainingCredentialLifetime, issuerCred, claims);
			return Response.ok().entity(token).build();
		}
		catch(SecurityException se) {
			return handleError(403, "Error creating token", se, logger);
		}
		catch(Exception ex) {
			return handleError("Error creating token", ex, logger);
		}
	}

	@DELETE
	@Path("/")
	public Response invalidateToken() throws Exception {
		if(isTokenAuth())
			try {
				String bearerToken = (String)AuthZAttributeStore.getTokens().getContext().
						get(AuthNHandler.UC_BEARER_TOKEN);
				JSONObject payload = JWTUtils.getPayload(bearerToken);
				String tokenID = payload.optString("jti", null);
				if(tokenID==null) {
					return Response.status(400).build();
				}
				deleteTokenInfo(tokenID);
				return Response.ok().build();
			}
		catch(Exception ex) {
			return handleError("Error deleting token", ex, logger);
		}
		else {
			return Response.status(403).build();
		}
	}

	public static final String service = "APIToken";

	private void deleteTokenInfo(String tokenID) throws Exception {
		kernel.getPersistenceManager().getInfoStore().remove(tokenID);

		// TBD it should be much easier to invalidate the current session
		SecurityTokens tokens = AuthZAttributeStore.getTokens();
		Object sessionID = tokens.getContext().get(SecuritySessionUtils.SESSION_ID_KEY);
		if(sessionID!=null) {
			String sID = (String)sessionID;
			SecuritySession s = kernel.getOrCreateSecuritySessionStore().getSession(sID);
			SecuritySession inv = new SecuritySession(sID, tokens, 0);
			inv.setUserKey(s.getUserKey());
			kernel.getOrCreateSecuritySessionStore().storeSession(inv, tokens);
			PostInvokeHandler.clearSession();
		}
	}

	private boolean isTokenAuth() {
		return "APITOKEN".equals(AuthZAttributeStore.getTokens().getContext().
				get(AuthNHandler.USER_AUTHN_METHOD));
	}

	/**
	 * creates a new API token and stores it
	 * @param user
	 * @param lifetime
	 * @param issuerCred
	 * @param claims
	 * @return
	 * @throws Exception
	 */
	public static String issueToken(Kernel kernel,
			String user, long lifetime, X509Credential issuerCred, Map<String,String> claims)
	throws Exception {
		Map<String,String> _claims = new HashMap<>();
		_claims.put("apitoken", "true");
		_claims.putAll(claims);
		String tokenID = UUID.newUniqueID();
		_claims.put("jti", tokenID);
		_claims.put("aud", issuerCred.getSubjectName());
		String token = JWTUtils.createJWTToken(user, lifetime,
				issuerCred.getSubjectName(), issuerCred.getKey(),
				_claims);
		Calendar tt = Calendar.getInstance();
		tt.setTimeInMillis(System.currentTimeMillis()+1000*lifetime);
		InstanceInfoBean info = new InstanceInfoBean(tokenID, service, tt);
		info.about = user;
		kernel.getPersistenceManager().getInfoStore().write(info);
		return token;
	}

	public static boolean checkTokenExists(Kernel kernel, String tokenID) throws Exception {
		InstanceInfoBean info = kernel.getPersistenceManager().getInfoStore().read(tokenID);
		return info!=null && service.equals(info.serviceName);
	}

	public static void runCleanup(Kernel kernel) throws Exception {
		Persist<InstanceInfoBean> store = kernel.getPersistenceManager().getInfoStore();
		List<String>tokenIDs = store.getIDs("service", service);
		int count = 0;
		for(String id: tokenIDs) {
			InstanceInfoBean info = store.read(id);
			if(System.currentTimeMillis()>info.date) {
				store.remove(id);
				count++;
			}
		}
		if(count>0)logger.info("Removed <{}> expired API tokens.", count);
	}

}