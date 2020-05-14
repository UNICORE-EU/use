package eu.unicore.services.rest.jwt;

import java.security.PublicKey;

import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.util.PubkeyCache;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.rest.security.jwt.JWTUtils;

public class JWTHelper {
	
	private final JWTServerProperties preferences;
	
	private final IContainerSecurityConfiguration securityProperties;
	
	private final String issuer;
	
	private final PubkeyCache keyCache;
	
	public JWTHelper(JWTServerProperties preferences, IContainerSecurityConfiguration securityProperties, PubkeyCache keyCache){
		this.preferences = preferences;
		this.securityProperties = securityProperties;
		this.issuer = securityProperties.getCredential()!=null?
				securityProperties.getCredential().getSubjectName() : Client.ANONYMOUS_CLIENT_DN;
		this.keyCache = keyCache;
	}
	
	public JWTHelper(JWTServerProperties preferences, String issuer, PubkeyCache keyCache){
		this.preferences = preferences;
		this.securityProperties = null;
		this.issuer = issuer;
		this.keyCache = keyCache;
	}
	
	public String createJWTToken(SecurityTokens tokens) throws Exception {
		return createETDToken(tokens.getEffectiveUserName());
	}

	public String createETDToken(String user) throws Exception {
		return createETDToken(user,preferences.getTokenValidity());
	}
	
	public String createETDToken(String user, long lifetime) throws Exception {
		if(preferences.useKey()){
			return JWTUtils.createETDToken(user, lifetime, issuer, 
					securityProperties.getCredential().getKey());
		}
		else{
			return JWTUtils.createETDToken(user, lifetime, issuer, 
					preferences.getHMACSecret());
		}
	}

	public void verifyJWTToken(String token) throws Exception {
		if(preferences.useKey()){
			String issuer = JWTUtils.getIssuer(token);
			PublicKey pk = keyCache.getPublicKey(issuer);
			if(pk==null) throw new AuthenticationException("No public key is available for <"+issuer+">");
			JWTUtils.verifyJWTToken(token, pk);
		}
		else{
			JWTUtils.verifyJWTToken(token, preferences.getHMACSecret());
		}
	}
	
}
