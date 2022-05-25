package eu.unicore.services.rest.registry;

import java.util.Properties;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryFeature;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.security.TestConfigUtil;

public class TestRegistryService {

	private static Kernel kernel;
	
	@BeforeClass
	public static void startServer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		kernel = new Kernel(p);
		kernel.startSynchronous();
		kernel.getDeploymentManager().deployFeature(kernel.load(RegistryFeature.class));
	}

	@AfterClass
	public static void stopServer()throws Exception{
		kernel.shutdown();
	}

	
	@Test
	public void testRegistry() throws Exception {
		BaseClient client = getClient();
		JSONObject o = client.getJSON();
		System.out.println("*** registry properties ***\n"+o.toString(2));
	}
	
	private BaseClient getClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		return new BaseClient(url, kernel.getClientConfiguration());
	}

}
