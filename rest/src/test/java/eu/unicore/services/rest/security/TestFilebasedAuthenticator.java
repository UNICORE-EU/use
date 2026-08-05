package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileWriter;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.security.FilebasedAuthenticator.AttributesHolder;
import eu.unicore.services.security.TestConfigUtil;

public class TestFilebasedAuthenticator {

	String demoUser = "CN=Demo User, O=UNICORE, C=EU";

	@Test
	public void testAuthenticator() throws Exception {
		Properties p = TestConfigUtil.getInsecureProperties();
		String file = "target/test-usermapfile.txt";
		try (FileWriter f = new FileWriter(file)){
			f.write(FilebasedAuthenticator.generateLine("demouser", "test123", demoUser));
		}
		p.setProperty("container.security.rest.authentication.order", "FILE");
		p.setProperty("container.security.rest.authentication.FILE.class", FilebasedAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.FILE.file", file);
		Kernel k = new Kernel(p);
		AuthenticatorChain chain = AuthenticatorChain.getAuthenticatorChain(k);
		assertTrue(chain.getChain().get(0) instanceof FilebasedAuthenticator);
		FilebasedAuthenticator fba = (FilebasedAuthenticator)chain.getChain().get(0);
		System.out.println(fba);
		assertEquals(file, fba.getFile());

		// check that authn works
		SecurityTokens tokens = new SecurityTokens();
		HTTPAuthNTokens http = new HTTPAuthNTokens("demouser", "test123");
		tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP, http);
		chain.authenticate(null,tokens);
		String dn = tokens.getEffectiveUserName();
		System.out.println("Authenticated DN : "+dn);
		assertEquals(demoUser,dn);
		// write-back
		assertTrue(fba.set(tokens, "foobar123"));
		// check old passwd fails
		tokens.setUserName(null);
		chain.authenticate(null,tokens);
		assertNull(tokens.getEffectiveUserName());
		// check auth with the new passwd works
		tokens = new SecurityTokens();
		http = new HTTPAuthNTokens("demouser", "foobar123");
		tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP, http);
		chain.authenticate(null,tokens);
		dn = tokens.getEffectiveUserName();
		assertEquals(demoUser,dn);
	}

	@Test
	public void testImmutable() throws Exception {
		Properties p = TestConfigUtil.getInsecureProperties();
		String file = "target/test-usermapfile.txt";
		try (FileWriter f = new FileWriter(file)){
			f.write(FilebasedAuthenticator.generateLine("demouser", "test123", demoUser));
		}
		p.setProperty("container.security.rest.authentication.order", "FILE");
		p.setProperty("container.security.rest.authentication.FILE.class", FilebasedAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.FILE.file", file);
		p.setProperty("container.security.rest.authentication.FILE.immutable", "true");
		Kernel k = new Kernel(p);
		AuthenticatorChain chain = AuthenticatorChain.getAuthenticatorChain(k);
		assertTrue(chain.getChain().get(0) instanceof FilebasedAuthenticator);
		FilebasedAuthenticator fba = (FilebasedAuthenticator)chain.getChain().get(0);
		System.out.println(fba);
		assertEquals(file, fba.getFile());
		// check that writing is disabled
		SecurityTokens tokens = new SecurityTokens();
		HTTPAuthNTokens http = new HTTPAuthNTokens("demouser", "test123");
		tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP, http);
		assertFalse(fba.set(tokens, "foobar123"));
	}

	@Test
	public void testGenerateLine() throws Exception {
		System.out.println("Sample password file line: "+FilebasedAuthenticator
				.generateLine("demouser", "test123", demoUser));
		String admin = "CN=Demo Admin";
		System.out.println("Sample password file line: "+FilebasedAuthenticator
				.generateLine("admin", "admin", admin));
		String user2 = "CN=Other User, O=UNICORE, C=EU";
		System.out.println("Sample password file line: "+FilebasedAuthenticator
				.generateLine("testuser", "321", user2));
	}

	@Test
	public void testAttribHolder(){
		String line = "user:hash:salt:CN=a:b";
		AttributesHolder ah = new AttributesHolder(line);
		assertEquals("user", ah.user);
		assertEquals("hash", ah.hash);
		assertEquals("salt", ah.salt);
		assertEquals("CN=a:b", ah.dn);
	}

}
