package eu.unicore.services.rest.jwt;

import java.io.IOException;

import org.apache.http.HttpMessage;

import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import eu.unicore.services.rest.client.IAuthCallback;

/**
 * Adds JWT delegation token to outgoing call. It will be signed with the server private key 
 * or shared secret
 *  
 * @author schuller
 */
public class JWTDelegation implements IAuthCallback {

	private final JWTServerProperties properties;
	private final JWTHelper jwtHelper;
	private final String user;
	
	private String token;
	
	private long issued; 
	
	/**
	 * @param security
	 * @param properties
	 * @param keyCache
	 * @param user - user identity to issue a JWT token for
	 */
	public JWTDelegation(IContainerSecurityConfiguration security, JWTServerProperties properties, String user){
		this.properties = properties;
		this.user = user;
		this.jwtHelper = new JWTHelper(properties, security, null);
	}
	
	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		httpMessage.removeHeaders("Authorization");
		httpMessage.addHeader("Authorization", "Bearer "+getToken());
	}
	
	public String getToken() throws Exception {
		if(token == null || tokenInvalid()){
			createToken();
		}
		return token;
	}
	
	protected void createToken() throws Exception {
		token = jwtHelper.createETDToken(user, properties.getTokenValidity());
		issued = System.currentTimeMillis();
	}
	
	protected boolean tokenInvalid() throws IOException {
		return issued+(500*properties.getTokenValidity())<System.currentTimeMillis();
	}

	@Override
	public String getSessionKey() {
		return user!=null? "JWTDELEGATION:"+user : null;
	}
}
