package eu.unicore.services.restclient.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Test;

import eu.unicore.security.AuthenticationException;
import eu.unicore.services.restclient.utils.UserLogger;
import eu.unicore.util.configuration.ConfigurationException;

public class TestTokenAuthN {

	@Test
	public void testTokenAuthN() throws Exception {
		TokenAuthN auth = new TokenAuthN();
		auth.setLogger(new UserLogger(){});
		HttpGet m = new HttpGet("http://foo");
		assertThrows(AuthenticationException.class, ()->auth.addAuthenticationHeaders(m));
		Properties p = new Properties();
		p.put("token", "test123");
		p.put("token-type", "Basic");
		auth.setProperties(p);
		auth.addAuthenticationHeaders(m);
		assertEquals("Basic test123", m.getHeader("Authorization").getValue());
		// read token from file
		p.put("token", "@src/test/resources/token.txt");
		auth.setProperties(p);
		assertEquals("test_token", auth.token);
		p.put("token", "@no_such_file");
		assertThrows(ConfigurationException.class, ()->auth.setProperties(p));
	}

}
