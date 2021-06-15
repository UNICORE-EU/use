package eu.unicore.services.registry.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.LocalRegistryEntryHomeImpl;
import eu.unicore.services.registry.LocalRegistryHomeImpl;
import eu.unicore.services.registry.RegistryCreator;
import eu.unicore.services.registry.RegistryEntryUpdater;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.ws.WSServerResource;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.services.ws.client.BaseWSRFClient;
import eu.unicore.services.ws.client.RegistryClient;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;
import eu.unicore.services.ws.sg.Registry;
import eu.unicore.services.ws.sg.ServiceGroupEntry;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestRegistryEntryUpdater extends JettyTestCase {

	@Before
	public void addServices() throws Exception{
		if(kernel.getService("example")!=null)return;
		CXFServiceFactory.createAndDeployService(kernel, "example", 
				WSServerResource.class, WSResourceHomeImpl.class, null);
		CXFServiceFactory.createAndDeployService(kernel, "ServiceGroupEntry", 
				ServiceGroupEntry.class, LocalRegistryEntryHomeImpl.class, null);
		CXFServiceFactory.createAndDeployService(kernel, "Registry", 
				Registry.class, LocalRegistryHomeImpl.class, null);
		RegistryHandler rh = new RegistryHandler(kernel);
		kernel.setAttribute(RegistryHandler.class, rh);
		new RegistryCreator(kernel).createRegistry();
	}

	@Test
	public void testRegistryEntryUpdater() throws Exception {
		EndpointReferenceType epr = createInstance();
		String ep = epr.getAddress().getStringValue();
		LocalRegistryClient registry = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
		
		String entryID = registry.addEntry(ep, SGFrontend.parse(epr), null);
		Collection<Map<String,String>>entries = registry.listEntries();
		assertNotNull(entries);
		assertTrue(entries.size()>0);
		RegistryEntryUpdater upd = new RegistryEntryUpdater();
		Home registryHome=kernel.getHome(ServiceGroupEntry.SERVICENAME);
		
		boolean expired = upd.check(registryHome, entryID);
		assertFalse(expired);
		
		//check if process() returns true (since the service is up)
		boolean up=upd.process(registryHome, entryID);
		assertTrue(up);
		
		//remove the service
		new BaseWSRFClient(epr, kernel.getClientConfiguration()).destroy();
		
		//and process() should be false now
		up=upd.process(registryHome, entryID);
		assertFalse(up);
		
		//and the registry entry should be gone
		try{
			new BaseWSRFClient(entryEPR(entryID), kernel.getClientConfiguration()).getCurrentTime();
			fail("Entry should be gone");
		}catch(Exception ex){
			/* OK */
		}
		
		entries=registry.listEntries();
		assertEquals(0, entries.size());
	}
	
	@Test
	public void testChangeBaseURL() throws Exception {
		EndpointReferenceType epr = createInstance();
		String ep = epr.getAddress().getStringValue();
		LocalRegistryClient registry = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
		String entryID = registry.addEntry(ep, SGFrontend.parse(epr), null);
		
		Collection<Map<String,String>>entries = registry.listEntries();
		assertNotNull(entries);
		assertTrue(entries.size()>0);
		
		String url=kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
		Map<String,String> e = entries.iterator().next();
		System.out.println(e);
		assertTrue(e.get(RegistryClient.ENDPOINT).contains(url));
		
		// change base url
		kernel.getContainerProperties().setProperty(ContainerProperties.EXTERNAL_URL, "x"+url);
		
		// entry should be now be invalid
		RegistryEntryUpdater upd=new RegistryEntryUpdater();
		Home registryHome=kernel.getHome(ServiceGroupEntry.SERVICENAME);
		boolean valid=upd.process(registryHome, entryID);
		assertFalse(valid);
		try{
			registryHome.get(entryID);
			fail("Entry should be gone");
		}catch(Exception ex){
			/* OK */
		}
		
		// reset
		kernel.getContainerProperties().setProperty(ContainerProperties.EXTERNAL_URL, url);
	}
	
	
	@Test
	public void testRemoveMultipleEntries()throws Exception{
		LocalRegistryClient registry = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
		
		int N=10;
		
		for(int i=0;i<N;i++){		
			String endpoint = "http://foo1/"+i;
			registry.addEntry(endpoint, new HashMap<>(), null);
		}
		
		int c=countEntries(registry, "foo1");
		assertEquals(N, c);
		
		DefaultHome registryHome=(DefaultHome)kernel.getHome(ServiceGroupEntry.SERVICENAME);
		assertNotNull(registryHome);
		registryHome.runExpiryCheckNow();
		
		c=countEntries(registry, "foo1");
		assertEquals(0, c);
	}
	
	private int countEntries(LocalRegistryClient reg, String memberUrlContains)throws Exception{
		int c=0;
		for(Map<String,String> e: reg.listEntries()){
			if(e.get(RegistryClient.ENDPOINT).contains(memberUrlContains)){
				c++;
			}
		}
		return c;
	}
	
	private EndpointReferenceType createInstance() throws Exception{
		//directly add an instance
		Home h=kernel.getHome("example");
		assertNotNull(h);
		InitParameters init = new InitParameters();
		String uid=h.createResource(init);
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(getBaseurl()+"/example?res="+uid);
		WSUtilities.addPortType(epr, new QName("foo","bar"));
		return epr;
	}
	
	private EndpointReferenceType entryEPR(String entryID) throws Exception{
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(getBaseurl()+"/example?res="+entryID);
		return epr;
	}
	
}
