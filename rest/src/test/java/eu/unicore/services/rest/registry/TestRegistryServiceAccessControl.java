package eu.unicore.services.rest.registry;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.security.pdp.DefaultPDP;

public class TestRegistryServiceAccessControl {

	private static Kernel kernel;

	private static Properties getProperties(){
		Properties p = new Properties();
		p.put("container.security.credential.path", "src/test/resources/keystore.jks");
		p.put("container.security.credential.password", "the!njs");
		p.put("container.security.truststore.type", "directory");
		p.put("container.security.truststore.directoryLocations.1", "src/test/resources/cacert.pem");
		p.put("container.security.accesscontrol.pdp", DefaultPDP.class.getName());
		p.put("container.host", "localhost");
		p.put("container.port", "55333");
		p.put("container.security.sslEnabled", "true");
		p.put("container.httpServer.fastRandom", "true");
		p.put("container.security.gateway.enable", "false");
		p.put("container.client.serverHostnameChecking", "NONE");
		p.put("persistence.directory", "target/data");
		// this makes sense only for testing: set self as external registry
		p.put("container.externalregistry.use", "true");
		p.put("container.externalregistry.url", "https://localhost:55333/rest/registries/default_registry");
		return p;
	}

	@BeforeEach
	public void startServer()throws Exception{
		FileUtils.deleteDirectory(new File("target/data"));
		Properties p = getProperties();
		kernel = new Kernel(p);
		kernel.startSynchronous();
		kernel.getDeploymentManager().deployFeature(kernel.load(RegistryFeature.class));
	}

	@AfterEach
	public void stopServer()throws Exception{
		kernel.getAttribute(RegistryHandler.class).getRegistryClient().invalidateCache();
		kernel.shutdown();
		FileUtils.deleteDirectory(new File("target/data"));
	}

	@Test
	public void testRegistryReadAccess() throws Exception {
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		rh.getRegistryClient().invalidateCache();
		// add something
		Map<String,String> content = new HashMap<>();
		content.put(RegistryImpl.INTERFACE_NAME, "http://spam");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://ham");
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
		rh.getRegistryClient().addEntry("http://foo", content, null);
		// check we have read access
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/registries";
		BaseClient bc = new BaseClient(url, kernel.getClientConfiguration());
		assertNotNull(bc.getJSON());
		url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
		bc.setURL(url);
		JSONObject j = bc.getJSON();
		assertNotNull(j);
		System.out.println(j.toString(2));
		// check read access on entry
		String eID = j.getJSONArray("entries").getJSONObject(0).getString("EntryID");
		url = kernel.getContainerProperties().getContainerURL()+"/rest/registryentries/"+eID;
		bc.setURL(url);
		j = bc.getJSON();
		assertNotNull(j);
		System.out.println(j.toString(2));
	}

}