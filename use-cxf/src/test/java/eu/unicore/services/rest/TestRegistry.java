package eu.unicore.services.rest;

import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.registry.RegistryEntryHomeImpl;
import eu.unicore.services.registry.RegistryHomeImpl;
import eu.unicore.services.registry.ws.SGEFrontend;
import eu.unicore.services.registry.ws.SGFrontend;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.ws.client.RegistryClient;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.sg.Registry;
import eu.unicore.services.ws.sg.ServiceGroupEntry;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestRegistry extends JettyTestCase {

	@Before
	public void addServices() throws Exception{
		CXFServiceFactory.createAndDeployService(kernel, "ServiceGroupEntry", 
				ServiceGroupEntry.class, RegistryEntryHomeImpl.class, 
				SGEFrontend.class.getName());
		CXFServiceFactory.createAndDeployService(kernel, "Registry", 
				Registry.class, RegistryHomeImpl.class,
				SGFrontend.class.getName());
		DeploymentDescriptorImpl info = new DeploymentDescriptorImpl();
		info.setName("registries");
		info.setType(RestService.TYPE);
		info.setImplementation(RegistryApplication.class);
		info.setKernel(kernel);
		kernel.getDeploymentManager().deployService(info);
		
		InitParameters init = new InitParameters("default_registry", TerminationMode.NEVER);
		kernel.getHome("Registry").createResource(init);
	}
	
	@Test
	public void test1() throws Exception {
		String url = kernel.getServer().getUrls()[0]+"/rest/registries/default_registry";
		BaseClient bc = new BaseClient(url, kernel.getClientConfiguration());
		System.out.println(bc.getJSON());
		// post an entry
		JSONObject entry = new JSONObject();
		entry.put(RegistryClient.ENDPOINT, "http://foo");
		entry.put(RegistryClient.INTERFACE_NAME, "foo");
		entry.put(RegistryClient.INTERFACE_NAMESPACE, "http://blah");
		entry.put(RegistryClient.SERVER_IDENTITY, "CN=test server");
		entry.put(RegistryClient.SERVER_PUBKEY, "not a public key");
		HttpResponse res = bc.post(entry);
		bc.checkError(res);
		bc.close(res);
		System.out.println(bc.getJSON());
	}
}
