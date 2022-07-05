package eu.unicore.services.rest.registry;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryFeature;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.security.TestConfigUtil;

public class TestRegistryService {

	private static Kernel kernel;
	
	@BeforeClass
	public static void startServer()throws Exception{
		FileUtils.deleteDirectory(new File("target/data"));
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
		FileUtils.deleteDirectory(new File("target/data"));
	}

	
	@Test
	public void testRegistry() throws Exception {
		BaseClient client = getClient();
		JSONObject o = client.getJSON();
		assert o.getJSONArray("entries").length()==0;

		// add something
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		Map<String,String> content = new HashMap<>();
		content.put(RegistryImpl.INTERFACE_NAME, "http://spam");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://ham");
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
		rh.getRegistryClient().addEntry("http://foo", content, null);
		o = client.getJSON();
		System.out.println("*** registry properties ***\n"+o.toString(2));
		assert o.getJSONArray("entries").length()==1;
	}
	
	private BaseClient getClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		return new BaseClient(url, kernel.getClientConfiguration());
	}

}
