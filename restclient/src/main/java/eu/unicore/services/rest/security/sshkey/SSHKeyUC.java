package eu.unicore.services.rest.security.sshkey;

import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.HttpMessage;

import eu.unicore.services.rest.client.IAuthCallback;

/**
 * UNICORE proprietary token used to verify that the user 
 * owns an accepted private key (SSH key)
 * 
 * TODO phase out in favor of JWT version {@link SSHKey}
 * 
 * @author schuller
 */
public class SSHKeyUC implements IAuthCallback {

	public static String HEADER_PLAINTEXT_TOKEN = "X-UNICORE-SSHKEY-AUTH-PLAINTEXT";
	
	public static final String TYPE="SSHKEY";

	public String username;

	/**
	 * Base64 encoded and sha1 hashed token
	 */
	public String token;

	/**
	 * Base64 encoded signature
	 */
	public String signature;

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		String basicAuth = "Basic "+new String(Base64.encodeBase64((username+":"+signature).getBytes()));
		httpMessage.setHeader("Authorization", basicAuth);
		httpMessage.setHeader(HEADER_PLAINTEXT_TOKEN,token);
	}
	
	public String toString(){
		return "[SSH auth "+username+":"+token+":"+signature+"]";
	}

}
