package eu.unicore.services.restclient;

import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.HttpMessage;

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