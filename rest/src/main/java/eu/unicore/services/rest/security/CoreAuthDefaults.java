package eu.unicore.services.rest.security;

import java.util.HashMap;
import java.util.Map;

public class CoreAuthDefaults implements AuthenticatorDefaults {

	private static final Map<String,String> defs = new HashMap<>();

	static {
		defs.put("X509", X509Authenticator.class.getName());
		defs.put("PAM", PAMAuthenticator.class.getName());
		defs.put("FILE", FilebasedAuthenticator.class.getName());
		defs.put("OAUTH", OAuthAuthenticator.class.getName());
		defs.put("UNITY-OAUTH", UnityOAuthAuthenticator.class.getName());
		defs.put("UNITY-PASSWORD", UnitySAMLAuthenticator.class.getName());
		defs.put("SSHKEY", SSHKeyAuthenticator.class.getName());
	}

	@Override
	public String getImplementationClass(String name) {
		return defs.get(name);
	}

}