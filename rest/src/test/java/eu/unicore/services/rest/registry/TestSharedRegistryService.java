package eu.unicore.services.rest.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.services.security.TestConfigUtil;

public class TestSharedRegistryService {

	private static Kernel kernel;

	@BeforeAll
	public static void startServer()throws Exception{
		FileUtils.deleteDirectory(new File("target/data"));
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		p.setProperty("container.feature.Registry.mode", "shared");
		kernel = new Kernel(p);
		kernel.startSynchronous();
		kernel.getDeploymentManager().deployFeature(kernel.load(RegistryFeature.class));
	}

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.getAttribute(RegistryHandler.class).getRegistryClient().invalidateCache();
		kernel.shutdown();
		FileUtils.deleteDirectory(new File("target/data"));
	}

	@Test
	public void testRegistry() throws Exception {
		RegistryClient registryClient = getRClient();
		assertEquals(0, registryClient.listEntries().size());
		Map<String,String> content = new HashMap<>();
		content.put(RegistryClient.INTERFACE_NAME, "http://spam");
		content.put(RegistryClient.INTERFACE_NAMESPACE, "http://ham");
		content.put(RegistryClient.ENDPOINT, "http://foo");
		registryClient.addEntry(content);
		assertEquals(1, registryClient.listEntries().size());
		((DefaultHome)kernel.getHome("Registry")).runExpiryCheckNow();
	
		ExternalRegistryClient extClient = getExtClient();
		System.out.println("Ext registry client: "+
						extClient.getConnectionStatus()+" " +extClient.getConnectionStatusMessage());
		assertEquals(1, extClient.listEntries().size());
		content.put(RegistryClient.INTERFACE_NAME, "http://spam2");
		content.put(RegistryClient.INTERFACE_NAMESPACE, "http://ham2");
		content.put(RegistryClient.ENDPOINT, "http://foo2");
		extClient.addRegistryEntry(content);
		kernel.getAttribute(RegistryHandler.class).getRegistryClient().invalidateCache();
		assertEquals(2, registryClient.listEntries().size());
	}
	
	private RegistryClient getRClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		return new RegistryClient(url, kernel.getClientConfiguration());
	}

	private ExternalRegistryClient getExtClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		return ExternalRegistryClient.getExternalRegistryClient(Arrays.asList(url), kernel.getClientConfiguration());
	}
}
