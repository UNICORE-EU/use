package eu.unicore.services.restclient.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class TestOIDCAgentAuthN  {

	@Test
	public void testOIDCAgentAuthN() throws Exception {
		var a = new OIDCAgentAuthN();
		a.setAgentProxy(new MockAP());
		var p = new Properties();
		p.setProperty("oidc-agent.account", "test");
		a.setProperties(p);
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		assertNotNull(h);
		assertEquals("Bearer some_access_token", h.getValue());
		a.lastRefresh = 0l;
		a.token = null;
		a.refreshTokenIfNecessary();
		assertTrue(a.lastRefresh>0);
		assertNotNull(a.token);
	}

	public static class MockAP extends OIDCAgentProxy {
		@Override
		public String send(String data) {
			JSONObject request = new JSONObject(data);
			assertEquals("test", request.getString("account"));
			JSONObject j = new JSONObject();
			j.put("status", "success");
			j.put("access_token", "some_access_token");
			j.put("refresh_token", "some_refresh_token");
			return j.toString();
		}
	}
}