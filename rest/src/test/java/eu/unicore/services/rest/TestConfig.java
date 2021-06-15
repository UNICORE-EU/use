package eu.unicore.services.rest;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.security.AuthenticatorChain;
import eu.unicore.services.rest.security.FilebasedAuthenticator;
import eu.unicore.services.rest.security.FilebasedAuthenticator.AttributesHolder;
import eu.unicore.services.rest.security.IAuthenticator;
import eu.unicore.services.rest.security.RESTSecurityProperties;
import eu.unicore.services.security.TestConfigUtil;

public class TestConfig {

	String demoUser = "CN=Demo User, O=UNICORE, C=EU";
	
	@Test
	public void testAuthNConfig() throws Exception {
		Properties p = new Properties();
		String file = "src/test/resources/users.txt";
		p.setProperty("container.security.rest.authentication.order", "FILE");
		p.setProperty("container.security.rest.authentication.FILE.class", FilebasedAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.FILE.file", file);
		RESTSecurityProperties props = new RESTSecurityProperties(new Kernel(TestConfigUtil.getInsecureProperties()),p);
		IAuthenticator auth = props.getAuthenticator();
		Assert.assertNotNull(auth);
		Assert.assertEquals(AuthenticatorChain.class, auth.getClass());
		AuthenticatorChain chain = (AuthenticatorChain)auth;
		Assert.assertEquals(1, chain.getChain().size());
		FilebasedAuthenticator fba = (FilebasedAuthenticator)chain.getChain().get(0);
		Assert.assertEquals(file,fba.getFile());
		
		// and finally test that authn works
		SecurityTokens tokens = new SecurityTokens();
		HTTPAuthNTokens http = new HTTPAuthNTokens("demouser", "test123");
		tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP, http);
		chain.authenticate(null,tokens);
		String dn = tokens.getEffectiveUserName();
		System.out.println("Authenticated DN : "+dn);
		Assert.assertEquals(demoUser,dn);
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
		Assert.assertEquals("user", ah.user);
		Assert.assertEquals("hash", ah.hash);
		Assert.assertEquals("salt", ah.salt);
		Assert.assertEquals("CN=a:b", ah.dn);
	}
}
