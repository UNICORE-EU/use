package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.jvnet.libpam.UnixUser;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.BasicAttributeHolder;
import eu.unicore.services.security.TestConfigUtil;

public class TestConfig {

	String demoUser = "CN=Demo User, O=UNICORE, C=EU";
	
	@Test
	public void testAuthNConfig() throws Exception {
		Properties p = TestConfigUtil.getInsecureProperties();
		String file = "src/test/resources/users.txt";
		p.setProperty("container.security.rest.authentication.order", "PAM FILE");
		p.setProperty("container.security.rest.authentication.PAM.dnTemplate", "CN=%s,OU=local-users");
		p.setProperty("container.security.rest.authentication.PAM.moduleName", "unicore");

		p.setProperty("container.security.rest.authentication.FILE.class", FilebasedAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.FILE.file", file);
		Kernel k = new Kernel(p);
		IAuthenticator auth = AuthenticatorChain.get(k);
		assertNotNull(auth);
		assertEquals(AuthenticatorChain.class, auth.getClass());
		AuthenticatorChain chain = (AuthenticatorChain)auth;
		assertEquals(2, chain.getChain().size());

		assertTrue(chain.getChain().get(0) instanceof PAMAuthenticator);
		FilebasedAuthenticator fba = (FilebasedAuthenticator)chain.getChain().get(1);
		System.out.println(fba);
		assertEquals(file,fba.getFile());

		// and finally test that authn works
		SecurityTokens tokens = new SecurityTokens();
		HTTPAuthNTokens http = new HTTPAuthNTokens("demouser", "test123");
		tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP, http);
		chain.authenticate(null,tokens);
		String dn = tokens.getEffectiveUserName();
		System.out.println("Authenticated DN : "+dn);
		assertEquals(demoUser,dn);
	}

	@Test
	public void testPAMAttributeForward() throws Exception {
		UnixUser u = new UnixUser("nobody");
		SecurityTokens t = new SecurityTokens();
		PAMAuthenticator.storePAMInfo(u, t);
		BasicAttributeHolder attrs = (BasicAttributeHolder)t.getContext().get(
				AuthAttributesCollector.ATTRIBUTES);
		assertEquals("nobody", attrs.uid);
	}
}
