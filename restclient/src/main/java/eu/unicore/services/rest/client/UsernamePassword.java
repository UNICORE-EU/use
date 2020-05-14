package eu.unicore.services.rest.client;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpMessage;

public class UsernamePassword implements IAuthCallback {

	private final String user;
	
	private final String password;
	
	public UsernamePassword(String user, String password) {
		this.user = user;
		this.password = password;
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		httpMessage.addHeader("Authorization", 
				"Basic "+new String(Base64.encodeBase64((user+":"+password).getBytes())));
	}
	
	@Override
	public String getSessionKey() {
		return "USERNAME:"+user+":"+password;
	}
	
}
