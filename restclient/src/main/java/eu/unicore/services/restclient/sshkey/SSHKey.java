package eu.unicore.services.restclient.sshkey;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;

import org.apache.hc.core5.http.HttpMessage;

import eu.emi.security.authn.x509.helpers.PasswordSupplier;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.jwt.JWTUtils;

/**
 * authenticate with a JWT token signed with a private key (SSH key)
 * 
 * @author schuller
 */
public class SSHKey implements IAuthCallback {

	private final File privateKey;
	private final PasswordSupplier password;
	private final String user;
	private final long lifetime; 
	
	private String token;
	
	private long issued; 
	
	public SSHKey(String user, File privateKey, PasswordSupplier password){
		this(user, privateKey, password, 300);
	}
	
	public SSHKey(String user, File privateKey, PasswordSupplier password, long lifetime){
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
		if(token == null || !tokenStillValid()) {
			createToken();
		}
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
