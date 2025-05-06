package eu.unicore.services.rest.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.rest.registry.RegistryHandler.RConnector;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.services.security.TestConfigUtil;

public class TestRegistryService {

	private static Kernel kernel;

	@BeforeAll
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

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.getAttribute(RegistryHandler.class).getRegistryClient().invalidateCache();
		kernel.shutdown();
		FileUtils.deleteDirectory(new File("target/data"));
	}

	@Test
	public void testRegistry() throws Exception {
		BaseClient client = getClient();
		JSONObject o = client.getJSON();
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		rh.getRegistryClient().invalidateCache();
		assertEquals(0, o.getJSONArray("entries").length());
		// add something
		Map<String,String> content = new HashMap<>();
		content.put(RegistryImpl.INTERFACE_NAME, "http://spam");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://ham");
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
		rh.getRegistryClient().addEntry("http://foo", content, null);
		o = client.getJSON();
		System.out.println("*** registry properties ***\n"+o.toString(2));
		assertEquals(1, o.getJSONArray("entries").length());
		RegistryClient registryClient = getRegistryClient();
		assertEquals(1, registryClient.listEntries().size());
		// add something else
		content.clear();
		content.put(RegistryClient.INTERFACE_NAME, "http://spam2");
		content.put(RegistryClient.INTERFACE_NAMESPACE, "http://ham2");
		content.put(RegistryClient.ENDPOINT, "http://foo2");
		registryClient.addEntry(content);
		o = client.getJSON();
		System.out.println("*** registry properties ***\n"+o.toString(2));
		assertEquals(2, o.getJSONArray("entries").length());
		// access entry
		JSONObject e1 = o.getJSONArray("entries").getJSONObject(0);
		String eID = e1.getString("EntryID");
		BaseClient eClient = getEntryClient();
		System.out.println("Entries: "+eClient.getJSON().toString(2));
		eClient.setURL(eClient.getURL()+"/"+eID);
		System.out.println("Entry properties: "+eClient.getJSON().toString(2));
		// delete
		eClient.delete();
		rh.getRegistryClient().invalidateCache();
		System.out.println("Registry handler status: '" +rh.getStatusDescription()+"'");
		registryClient = getRegistryClient();
		assertEquals(1, registryClient.listEntries().size());
	}

	@Test
	public void testRConnector() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		RConnector rc = new RConnector(url, kernel);
		System.out.println(String.format("Connector to %s: %s (msg: %s)" , rc.getExternalSystemName(), rc.getConnectionStatus(),
				rc.getConnectionStatusMessage()));
	}

	@Test
	public void testRegistryEntryUpdater() throws Exception {
		BaseClient client = getClient();
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		// add stuff
		RegistryClient registryClient = getRegistryClient();
		Map<String,String> content = new HashMap<>();
		content.put(RegistryImpl.INTERFACE_NAME, "http://spam");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://ham");
		content.put(RegistryImpl.ENDPOINT, "http://foo");	
		registryClient.addEntry(content);
		content = new HashMap<>();
		content.put(RegistryImpl.INTERFACE_NAME, "http://spam2");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://ham2");
		content.put(RegistryImpl.ENDPOINT, "http://foo2");	
		registryClient.addEntry(content);
		JSONObject o = client.getJSON();
		assertEquals(2, o.getJSONArray("entries").length());
		// run expire check manually - entry should be removed as it's not valid
		((DefaultHome)kernel.getHome("ServiceGroupEntry")).runExpiryCheckNow();
		rh.getRegistryClient().invalidateCache();
		assertEquals(0, registryClient.listEntries().size());
	}

	private BaseClient getClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		return new BaseClient(url, kernel.getClientConfiguration());
	}

	private RegistryClient getRegistryClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		return new RegistryClient(url, kernel.getClientConfiguration());
	}
	

	private BaseClient getEntryClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registryentries";
		return new BaseClient(url, kernel.getClientConfiguration());
	}

}
