package eu.unicore.services.rest.security.sshkey;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;

import org.apache.http.HttpMessage;

import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.security.jwt.JWTUtils;

/**
 * authenticate with a JWT token signed with a private key (SSH key)
 * 
 * @author schuller
 */
public class SSHKey implements IAuthCallback {

	private final File privateKey;
	private final Password password;
	private final String user;
	private final long lifetime; 
	
	private String token;
	
	private long issued; 
	
	public SSHKey(String user, File privateKey, Password password){
		this(user, privateKey, password, 300);
	}
	
	public SSHKey(String user, File privateKey, Password password, long lifetime){
		this.user = user;
		this.privateKey = privateKey;
		this.password = password;
		this.lifetime = lifetime;
	}
	
	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		httpMessage.removeHeaders("Authorization");
		httpMessage.addHeader("Authorization", "Bearer "+getToken());
	}
	
	public String getToken() throws Exception {
		if(token == null)createToken();
		if(!tokenStillValid())createToken();
		return token;
	}
	
	protected void createToken() throws Exception {
		final PrivateKey pk = SSHUtils.readPrivateKey(privateKey, password);
		token = JWTUtils.createJWTToken(user, lifetime, user, pk, null);
		issued = System.currentTimeMillis();
	}
	
	protected boolean tokenStillValid() throws IOException {
		return issued+(500*lifetime)>System.currentTimeMillis();
	}
	
}
