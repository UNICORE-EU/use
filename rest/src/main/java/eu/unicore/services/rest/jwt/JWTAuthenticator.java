package eu.unicore.services.rest.jwt;

import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.KernelInjectable;
import de.fzj.unicore.wsrflite.security.util.PubkeyCache;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.rest.security.IAuthenticator;
import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.util.Log;

/**
 * Checks the incoming message for a JWT auth token. If present it is validated.
 * On successful validation, the user is authenticated.
 * 
 * @author schuller
 */
public class JWTAuthenticator implements IAuthenticator, KernelInjectable {

	private static final Logger logger =  Log.getLogger(Log.SECURITY,JWTAuthenticator.class);
	
	private final static Collection<String> s = Collections.singletonList("Bearer");
	
	// TODO future extension where end users authenticate with JWT tokens
	// signed with their private key, with UNICORE reading the pub key
	// from the system (TSI / uftpd)
	@SuppressWarnings("unused")
	private String dnTemplate = "CN=%s, OU=sshkey-local-users";

	private JWTHelper jwt;
	
	@Override
	public void setKernel(Kernel k){
		JWTServerProperties p = new JWTServerProperties(k.getContainerProperties().getRawProperties());
		jwt = new JWTHelper(p, k.getContainerSecurityConfiguration(), PubkeyCache.get(k));
	}
	
	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	@Override
	public boolean authenticate(Message message, SecurityTokens tokens) {
		
		String bearerToken = CXFUtils.getBearerToken(message);
		if(bearerToken == null)return false;
		
		try{
			if(!validate(bearerToken, tokens))return false;
		}catch(Exception ex){
			String msg = Log.createFaultMessage("JWT token could not be validated.", ex);
			logger.warn(msg);
			throw new AuthenticationException(msg);
		}
		return true;
	}
	
	/**
	 * check syntax and validate
	 * 
	 * @return false if the token is not a well-formed JWT token
	 */
	protected boolean validate(String bearerToken, SecurityTokens tokens) throws Exception {
		JSONObject payload;
		// syntax check first
		try{
			payload = JWTUtils.getPayload(bearerToken);
		}catch(Exception jtd){
			return false;
		}
		jwt.verifyJWTToken(bearerToken);
		JSONObject json = new JSONObject(payload);
		String exp = json.optString("exp", null);
		if(exp==null)throw new AuthenticationException("JWT Token does not specify a lifetime ('exp' attribute).");
		String subject = json.getString("sub");
		String issuer = json.getString("iss");
		if(!subject.equals(issuer)){
			// delegation - not handled here, but in AuthHandler
			throw new IllegalStateException("Subject and issuer do not match.");
		}
		tokens.setUserName(subject);
		tokens.setConsignorTrusted(true);
		
		return true;
	}
	

	public void setDNTemplate(String dnTemplate){
		this.dnTemplate = dnTemplate;
	}
	
	public String toString(){
		return "JWT";
	}
	
}
