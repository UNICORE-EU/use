package eu.unicore.services.registry.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.sg2.AddDocument;
import org.oasisOpen.docs.wsrf.sg2.AddResponseDocument;
import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.registry.ServiceRegistryImpl;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import eu.unicore.services.registry.ws.RegistryFeature;
import eu.unicore.services.registry.ws.SGFrontend;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestWSRegistry extends JettyTestCase {

	@Before
	public void addServices() throws Exception{
		kernel.getDeploymentManager().deployFeature(new RegistryFeature(kernel));
	}

	@Test
	public void testWSFrontend() throws Exception {
		createRegistry();
		Map<String,String>content = new HashMap<>();
		content.put("foo","bar");
		content.put(RegistryClient.SERVER_IDENTITY,"CN=Server");
		content.put(RegistryClient.INTERFACE_NAME,"SomeInterface");
		content.put(RegistryClient.INTERFACE_NAMESPACE,"SomeNamespace");
		addContent(content);
		RegistryClient cl = createClient();
		List<EntryType> entries = cl.listEntries(); 
		assertEquals(1, entries.size());
		BaseWSRFClient entryClient = createEntryClient(entries.get(0).getServiceGroupEntryEPR());
		System.out.println(entryClient.getCurrentTime());
		EndpointReferenceType memberEPR = entries.get(0).getMemberServiceEPR();
		assertEquals("CN=Server", WSUtilities.extractServerIDFromEPR(memberEPR));
		QName port = WSUtilities.extractInterfaceName(memberEPR);
		assertEquals("SomeInterface", port.getLocalPart());
		assertEquals("SomeNamespace", port.getNamespaceURI());
		
		// test add via RegistryClient
		AddDocument in = AddDocument.Factory.newInstance();
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://foo");
		WSUtilities.addPortType(epr, new QName("SomeNamespace", "SomeOtherInterface"));
		in.addNewAdd().setMemberEPR(epr);
		AddResponseDocument res = cl.addRegistryEntry(in);
		assertNotNull(res.getAddResponse().getTerminationTime());
	
		EndpointReferenceType epr1 = EndpointReferenceType.Factory.newInstance();
		epr1.addNewAddress().setStringValue("http://foo");
		WSUtilities.addServerIdentity(epr1, "CN=Server");
		WSUtilities.addServerPublicKey(epr1, "--begin not a pem --\nfoo\n--end--\n");
		content = SGFrontend.parse(epr1);
		assertEquals("CN=Server", content.get(RegistryClient.SERVER_IDENTITY));
	}
	
	protected RegistryClient createClient()throws Exception{
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(getBaseurl()+"/Registry?res=default_registry");
		return new RegistryClient(epr, kernel.getClientConfiguration());
	}
	
	
	protected BaseWSRFClient createEntryClient(EndpointReferenceType epr)throws Exception{
		return new BaseWSRFClient(epr, kernel.getClientConfiguration());
	}
	
	protected void createRegistry() throws Exception {
		InitParameters init = new InitParameters("default_registry", TerminationMode.NEVER);
		kernel.getHome("Registry").createResource(init);
	}
	
	protected void addContent(Map<String,String>content) throws Exception {
		ServiceRegistryImpl reg = (ServiceRegistryImpl)kernel.getHome("Registry").getForUpdate("default_registry");
		reg.addEntry("http://foo", content, null);
		kernel.getHome("Registry").persist(reg);
	}
}
