package eu.unicore.services.restclient.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.util.httpclient.DefaultClientConfiguration;

public class TestOIDCServerAuthN {

	MockOIDCServer server;
	File tokenfile = new File("target","test-refresh-tokens-file");

	@BeforeEach
	public void startServer()throws Exception{
		server = new MockOIDCServer ();
		server.start();
		Thread.sleep(1000);
		FileUtils.deleteQuietly(tokenfile);
	}

	@AfterEach
	public void stopServer()throws Exception{
		server.stop();
	}

	@Test
	public void testAuthN() throws Exception {
		Properties p = new Properties();
		String prefix = "oidc.";
		p.setProperty(prefix + OIDCProperties.TOKEN_ENDPOINT, server.getURI());
		p.setProperty(prefix + OIDCProperties.CLIENT_ID, "test");
		p.setProperty(prefix + OIDCProperties.CLIENT_SECRET, "123");
		p.setProperty(prefix + OIDCProperties.USERNAME, "demouser");
		p.setProperty(prefix + OIDCProperties.PASSWORD, "test123");
		p.setProperty(prefix + OIDCProperties.OTP, "989898");
		p.setProperty(prefix + OIDCProperties.REFRESH_TOKEN_FILENAME, tokenfile.getAbsolutePath());
		OIDCProperties props = new OIDCProperties(p);
		DefaultClientConfiguration cc = new DefaultClientConfiguration();
		cc.setHttpAuthn(false);
		cc.setSslEnabled(false);
		cc.setSslAuthn(false);
		OIDCServerAuthN authn = new OIDCServerAuthN(props, cc);
		authn.retrieveToken();
		assertEquals("the_access_token", authn.token);
		assertEquals("the_refresh_token", authn.refreshToken);
		try(FileInputStream fis = new FileInputStream(tokenfile)){
			JSONObject refreshTokens = new JSONObject(IOUtils.toString(fis, "UTF-8"));
			String t = refreshTokens.getJSONObject(server.getURI()).getString("refresh_token");
			assertEquals("the_refresh_token", t);
		}
		// force refresh
		authn.lastRefresh = 0;
		authn.refreshTokenIfNecessary();
		assertEquals("the_new_access_token", authn.token);

		HttpGet h = new HttpGet("foo");
		authn.addAuthenticationHeaders(h);
		assertEquals("Bearer "+authn.token, h.getHeader("Authorization").getValue());
		assertEquals("demouser@"+server.getURI(), authn.getSessionKey());
		assertFalse(props.queryOTP());
	}
}