package eu.unicore.services.rest.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.services.Kernel;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.rest.registry.RegistryHandler.RConnector;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.services.security.util.PubkeyCache;

public class TestRegistryService {

	private static Kernel kernel;

	private static Properties getProperties(){
		Properties p = new Properties();
		p.put("container.security.credential.path", "src/test/resources/keystore.jks");
		p.put("container.security.credential.password", "the!njs");
		p.put("container.security.truststore.type", "directory");
		p.put("container.security.truststore.directoryLocations.1", "src/test/resources/cacert.pem");
		p.put("container.security.accesscontrol", "false");
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
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
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
		System.out.println(registryClient.listEntries());
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
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
		registryClient.addEntry(content);
		content = new HashMap<>();
		content.put(RegistryImpl.INTERFACE_NAME, "http://spam2");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://ham2");
		content.put(RegistryImpl.ENDPOINT, "http://foo2");	
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
		registryClient.addEntry(content);
		JSONObject o = client.getJSON();
		assertEquals(2, o.getJSONArray("entries").length());
		// add valid entry
		content.put(RegistryImpl.INTERFACE_NAME, "http://bar");
		content.put(RegistryImpl.INTERFACE_NAMESPACE, "http://foo");
		content.put(RegistryImpl.ENDPOINT, "https://localhost:55333/rest/registries/default_registry");	
		X509Credential cred = kernel.getContainerSecurityConfiguration().getCredential();
		content.put(RegistryImpl.SERVER_IDENTITY, cred.getSubjectName());	
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		CertificateUtils.saveCertificate(os, 
				kernel.getContainerSecurityConfiguration().getCredential().getCertificate(), 
				Encoding.PEM);
		content.put(RegistryImpl.SERVER_PUBKEY, os.toString("UTF-8"));
		content.put(RegistryImpl.MARK_ENTRY_AS_INTERNAL, "true");
		registryClient.addEntry(content);
		// run expire check manually to remove invalid entries
		((DefaultHome)kernel.getHome("ServiceGroupEntry")).runExpiryCheckNow();
		rh.getRegistryClient().invalidateCache();
		// valid entry should still be there
		assertEquals(1, registryClient.listEntries().size());
	}
	
	@Test
	public void testRegistryHandler() throws Exception {
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		assertFalse(rh.isSharedRegistry());
		System.out.println(kernel.getConnectionStatus());
		final List<Map<String,String>> entries = new ArrayList<>();
		ExternalRegistryClient erc = new ExternalRegistryClient() {
			@Override
			public List<Map<String,String>> listEntries() throws IOException {
				return entries;
			}
		};
		Map<String,String>e = new HashMap<>();
		e.put("InternalEntry", "true");
		String subj = "CN=Demo CA,O=UNICORE,C=EU";
		e.put(RegistryClient.SERVER_IDENTITY, subj);
		String pem = FileUtils.readFileToString(new File("src/test/resources/cacert.pem"), "UTF-8");
		e.put(RegistryClient.SERVER_PUBKEY, pem);
		entries.add(e);
		Map<String,String>e2 = new HashMap<>();
		e2.put(RegistryClient.SERVER_IDENTITY, "CN=Wrong");
		String pem2 = "not a pem";
		e2.put(RegistryClient.SERVER_PUBKEY, pem2);
		e.put("InternalEntry", "true");
		entries.add(e2);
		rh.doUpdateKeys(erc);
		PubkeyCache pc = PubkeyCache.get(kernel);
		assertTrue(pc.getPublicKeys(subj).size()>0);
		assertEquals(0, pc.getPublicKeys("CN=Wrong").size());
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
