package eu.unicore.services.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.registry.LocalRegistryClient;
import de.fzj.unicore.wsrflite.registry.ServiceRegistryImpl;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TestNormalStartup {

	static Kernel kernel;
	
	@BeforeClass
	public static void setup()throws Exception{
		FileUtils.deleteQuietly(new File("target","data"));
		if(kernel!=null)return;
		kernel=new Kernel("src/test/resources/conf/use.properties");
		kernel.startSynchronous();
	}
	
	@AfterClass
	public static void shutdown()throws Exception{
		if(kernel!=null)kernel.shutdown();
	}

	@Test
	public void testLocalRegistry()throws Exception{
		createInstance();
		RegistryClient c=getRegistryClient();
		c.getCurrentTime();
		Assert.assertTrue(c.listEntries().size()>0);
		// REST
		String base = kernel.getContainerProperties().getContainerURL();
		String regURL = base+"/rest/registries/default_registry";
		BaseClient bc = new BaseClient(base, getClientConfiguration());
		bc.setURL(base+"/rest/registries");
		System.out.println(bc.getJSON().toString(2));
		bc.setURL(regURL);
		System.out.println(bc.getJSON().toString(2));
	}
	
	private RegistryClient getRegistryClient() throws Exception {
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		String uri = getBaseURL()+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry";
		epr.addNewAddress().setStringValue(uri);
		RegistryClient c= new RegistryClient(epr, getClientConfiguration());
		c.setUpdateInterval(-1);
		return c;
	}
	
	private void createInstance() throws Exception{
		Home h=kernel.getHome("example");
		assertNotNull(h);
		InitParameters init = new InitParameters("test123");
		String uid=h.createResource(init);
		assertEquals("test123",uid);
		
		String endpoint = kernel.getContainerProperties().getBaseUrl()+"/example?res="+uid;
		LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
		Map<String,String>content = new HashMap<>();
		content.put(ServiceRegistryImpl.INTERFACE_NAME,"example");
		content.put(ServiceRegistryImpl.INTERFACE_NAMESPACE,"http://foo");
		lrc.addEntry(endpoint, content, null);
	}

	private String getBaseURL(){
		return kernel.getContainerProperties().getBaseUrl();
	}
	
	private IClientConfiguration getClientConfiguration(){
		return kernel.getClientConfiguration();
	}
}
