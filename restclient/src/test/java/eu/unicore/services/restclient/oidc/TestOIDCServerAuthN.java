package eu.unicore.services.restclient.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.util.httpclient.DefaultClientConfiguration;

public class TestOIDCServerAuthN {

	static MockOIDCServer server;
	static File tokenfile = new File("target","test-refresh-tokens-file");

	@BeforeAll
	public static void startServer()throws Exception{
		server = new MockOIDCServer ();
		server.start();
		Thread.sleep(1000);
	}

	@AfterAll
	public static void stopServer()throws Exception{
		server.stop();
		FileUtils.deleteQuietly(tokenfile);
	}

	@Test
	public void testAuthN() throws Exception {
		FileUtils.deleteQuietly(tokenfile);
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
		assertFalse(props.queryOTP());
		DefaultClientConfiguration cc = new DefaultClientConfiguration();
		cc.setHttpAuthn(false);
		cc.setSslEnabled(false);
		cc.setSslAuthn(false);
		OIDCServerAuthN authn = new OIDCServerAuthN(props, cc);
		authn.retrieveToken();
		assertEquals("test123", server.getParams().get("password"));
		assertEquals("989898", server.getParams().get("otp"));
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

	@Test
	public void testWithCallbacks() throws Exception {
		FileUtils.deleteQuietly(tokenfile);
		Properties p = new Properties();
		String prefix = "oidc.";
		p.setProperty(prefix + OIDCProperties.TOKEN_ENDPOINT, server.getURI());
		p.setProperty(prefix + OIDCProperties.CLIENT_ID, "test");
		p.setProperty(prefix + OIDCProperties.CLIENT_SECRET, "123");
		p.setProperty(prefix + OIDCProperties.USERNAME, "demouser");
		p.setProperty(prefix + OIDCProperties.OTP, "QUERY");
		p.setProperty(prefix + OIDCProperties.REFRESH_TOKEN_FILENAME, tokenfile.getAbsolutePath());
		OIDCProperties props = new OIDCProperties(p);
		assertTrue(props.queryOTP());
		DefaultClientConfiguration cc = new DefaultClientConfiguration();
		cc.setHttpAuthn(false);
		cc.setSslEnabled(false);
		cc.setSslAuthn(false);
		OIDCServerAuthN authn = new OIDCServerAuthN(props, cc);
		authn.setPasswordCallback(()->"test123");
		authn.setOTPCallback(()->"989898");
		authn.retrieveToken();
		assertEquals("test123", server.getParams().get("password"));
		assertEquals("989898", server.getParams().get("otp"));
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
	}
	
	@Test
	public void testLoadRefreshToken() throws Exception {
			FileUtils.deleteQuietly(tokenfile);
			JSONObject refresh = new JSONObject();
			refresh.put(server.getURI(), new JSONObject("{'refresh_token': 'test_refresh_token'}"));
			FileUtils.write(tokenfile, refresh.toString(), "UTF-8");
			Properties p = new Properties();
			String prefix = "oidc.";
			p.setProperty(prefix + OIDCProperties.TOKEN_ENDPOINT, server.getURI());
			p.setProperty(prefix + OIDCProperties.CLIENT_ID, "test");
			p.setProperty(prefix + OIDCProperties.CLIENT_SECRET, "123");
			p.setProperty(prefix + OIDCProperties.USERNAME, "demouser");
			p.setProperty(prefix + OIDCProperties.PASSWORD, "test123");
			p.setProperty(prefix + OIDCProperties.OTP, "989898");
			p.setProperty(prefix + OIDCProperties.REFRESH_TOKEN_FILENAME, tokenfile.getAbsolutePath());
			p.setProperty(prefix + OIDCProperties.STORE_REFRESH_TOKEN, "true");
			OIDCProperties props = new OIDCProperties(p);
			DefaultClientConfiguration cc = new DefaultClientConfiguration();
			cc.setHttpAuthn(false);
			cc.setSslEnabled(false);
			cc.setSslAuthn(false);
			OIDCServerAuthN authn = new OIDCServerAuthN(props, cc);
			assertEquals("test_refresh_token", authn.refreshToken);
	}
}