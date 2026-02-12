package eu.unicore.services.rest.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.security.TestConfigUtil;

public class TestAdminService {

	private static Kernel kernel;
	
	@BeforeAll
	public static void startServer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		kernel = new Kernel(p);
		kernel.startSynchronous();
		kernel.getDeploymentManager().deployFeature(new AdminFeature(kernel));
	}

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.shutdown();
	}

	@Test
	public void testListAdminActions()throws Exception{
		BaseClient client = getClient();
		JSONObject o = client.getJSON().getJSONObject("_links");
		JSONObject mock = o.getJSONObject("action:mock");
		System.out.println(mock.toString(2));
		assertTrue(mock.getString("description").contains("echoes incoming parameters"));
	}
	
	@Test
	public void testInvokeAdminAction()throws Exception{
		BaseClient client = getClient();
		JSONObject mock = client.getJSON().getJSONObject("_links").getJSONObject("action:mock");
		String url = mock.getString("href");
		client.setURL(url);
		JSONObject params = new JSONObject();
		params.put("k1","v1");
		params.put("k2","v2");
		JSONObject resp = client.asJSON(client.post(params));
		System.out.println(resp);
		assertTrue(resp.getBoolean("success"));
		assertEquals("ok", resp.getString("message"));
		JSONObject results = resp.getJSONObject("results");
		assertEquals(2, results.length());
		assertEquals("echo-v1",results.getString("k1"));
		assertEquals("echo-v2",results.getString("k2"));
	}

	private BaseClient getClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/admin";
		return new BaseClient(url, kernel.getClientConfiguration());
	}

}
