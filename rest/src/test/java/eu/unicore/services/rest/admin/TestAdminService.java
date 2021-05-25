package eu.unicore.services.rest.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import eu.unicore.services.rest.client.BaseClient;

public class TestAdminService {

	private Kernel kernel;
	
	@Before
	public void startServer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		kernel = new Kernel(p);
		kernel.startSynchronous();
		kernel.getDeploymentManager().deployFeature(new AdminFeature(kernel));
	}

	@After
	public void stopServer()throws Exception{
		kernel.shutdown();
	}

	
	@Test
	public void testGetAllMetrics() throws Exception {
		BaseClient client = getClient();
		JSONObject o = client.getJSON().getJSONObject("metrics");
		System.out.println(o.toString(2));
	}
	
	@Test
	public void testGetSingleMetric() throws Exception {
		BaseClient client = getClient();
		JSONObject o = client.getJSON().getJSONObject("metrics");
		String value = o.getString("use.rest.callFrequency");
		assertNotNull(value);
		System.out.println(value);
	}

	@Test
	public void testListAdminActions()throws Exception{
		BaseClient client = getClient();
		JSONObject o = client.getJSON().getJSONObject("_links");
		JSONObject mock = o.getJSONObject("action:mock");
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
