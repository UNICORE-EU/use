package eu.unicore.services.ws;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlOptions;
import org.junit.Before;
import org.junit.Test;
import org.unigrids.services.atomic.types.PermitDocument;
import org.unigrids.services.atomic.types.SecurityDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.SecuredResourceImpl;
import eu.unicore.services.registry.LocalRegistryEntryHomeImpl;
import eu.unicore.services.registry.LocalRegistryHomeImpl;
import eu.unicore.services.registry.RegistryCreator;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.ws.client.TestClientSSL;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.renderers.AddressRenderer;
import eu.unicore.services.ws.renderers.FixedAddressRenderer;
import eu.unicore.services.ws.renderers.SecurityInfoRenderer;
import eu.unicore.services.ws.sg.Registry;
import eu.unicore.services.ws.sg.ServiceGroupEntry;
import eu.unicore.services.ws.testutils.AbstractClientTest;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TestSecurityInfoRenderer extends AbstractClientTest {
	public static final String PCS = ContainerSecurityProperties.PREFIX;

	public String getBaseurl(){
		return "https://localhost:"+getPort()+"/services";
	}

	@Before
	public void addServices() throws Exception{
		super.addServices();
		if(kernel.getService("Registry")!=null)return;
		CXFServiceFactory.createAndDeployService(kernel,"Registry", Registry.class,
				LocalRegistryHomeImpl.class,null);
		CXFServiceFactory.createAndDeployService(kernel,"ServiceGroupEntry",ServiceGroupEntry.class,
				LocalRegistryEntryHomeImpl.class,null);
		RegistryHandler rh=new RegistryHandler(kernel);
		kernel.setAttribute(RegistryHandler.class, rh);
		new RegistryCreator(kernel).createRegistry();
		createTestResource();
	}

	protected IClientConfiguration getClientSideSecurityProperties() throws IOException{
		Properties p = new Properties();
		p.load(new StringReader(TestClientSSL.props));
		ClientProperties ret = new ClientProperties(p);
		ret.getETDSettings().getRequestedUserAttributes2().put(
				IAttributeSource.ATTRIBUTE_SELECTED_VO, new String[] {"/vo2"});
		return ret;
	}

	@Override
	protected Properties getServerSideSecurityProperties() throws IOException{
		Properties p = new Properties();
		String serverprops = TestClientSSL.serverprops + "\n" +
				PCS+ContainerSecurityProperties.PROP_AIP_ORDER+"=MOCK-AIP\n" +
				PCS+ContainerSecurityProperties.PROP_AIP_PREFIX+".MOCK-AIP.class=" 
				+ MockAIP.class.getName();
		p.load(new StringReader(serverprops));
		return p;
	}

	private Resource getTestResource()throws Exception{
		Home home=kernel.getHome("service");
		return home.get(WSUtilities.extractResourceID(client.getUrl()));
	}

	@Test
	public void testSecurityInfoRenderer() throws Exception{
		String rps = client.getResourcePropertyDocument();

		System.out.println("Reply: "+rps);
		assertEquals(1, numberOfOcurrences(rps, "typ:AcceptedCA>CN=Demo CA,O=UNICORE,C=EU<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Xgroup>gid1<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Xgroup>gid3<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Xgroup>gid2<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Xgroup>gid4<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Xlogin>uid1<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Xlogin>uid2<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Role>ala<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:Role>tola<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:UseOSDefaults>false<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:PrimaryGroup>gid2<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:SupplementaryGroup>gid1<"));
		assertEquals(1, numberOfOcurrences(rps, "typ:SupplementaryGroup>gid4<"));

	}

	private int numberOfOcurrences(String where, String what) {
		int ret = 0;
		int last = 0;
		while ((last=where.indexOf(what, last)) != -1) {
			ret++;
			last++;
		}
		return ret;
	}

	@Test
	public void testAddServerCert() throws Exception {
		SecuredResourceImpl res=(SecuredResourceImpl)getTestResource();
		res.getModel().setPublishServerCert(true);
		SecurityInfoRenderer r=new SecurityInfoRenderer(res);
		SecurityDocument xo=r.render()[0];
		XmlOptions opts=new XmlOptions();
		opts.setSavePrettyPrint();
		String rps = xo.xmlText(opts);
		assertTrue(rps.toString().contains("BEGIN CERTIFICATE"));
		assertTrue(xo.getSecurity().getServerDN().contains("CN=Demo UNICORE/X"));
	}

	@Test
	public void testAddressRendererWithServerID()throws Exception{
		Resource r=new MockWSResourceImpl();
		r.setKernel(kernel);
		final String s="testService?res=123";
		QName docName=EndpointReferenceDocument.type.getDocumentElementName();
		AddressRenderer rp=new FixedAddressRenderer(r,docName,s,true);
		EndpointReferenceDocument x=(EndpointReferenceDocument)rp.render()[0];
		assertTrue("Missing server ID, got: "+x, x.toString().contains("ServerIdentity"));
	}
	
	@Test
	public void testACL() throws Exception {
		assertEquals(1,client.getShares().getShare().getPermitArray().length);
		PermitDocument.Permit acl1 = PermitDocument.Permit.Factory.newInstance();
		acl1.setAllow("read");
		acl1.setWhen("DN");
		acl1.setIs("CN=Bob");
		client.addShare(acl1);
		assertEquals(2,client.getShares().getShare().getPermitArray().length);
		System.out.println(client.getResourcePropertyDocument());
	}
	
	public static class MockAIP implements IAttributeSource {

		@Override
		public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
				SubjectAttributesHolder otherAuthoriserInfo) throws IOException	{

			SubjectAttributesHolder ret = new SubjectAttributesHolder(otherAuthoriserInfo.getPreferredVos());
			Map<String, String[]> valid = new HashMap<String, String[]>();
			Map<String, String[]> defs = new HashMap<String, String[]>();

			valid.put(IAttributeSource.ATTRIBUTE_ROLE, new String[] {"ala", "tola"});
			valid.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[] {"uid1", "uid2"});
			valid.put(IAttributeSource.ATTRIBUTE_VOS, new String[] {"/vo2", "/vo1"});
			valid.put(IAttributeSource.ATTRIBUTE_GROUP, new String[] {"gid1", "gid2"});
			valid.put(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, new String[] {"gid1", "gid2", "gid3", "gid4"});
			valid.put(IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS, new String[] {"false"});

			defs.put(IAttributeSource.ATTRIBUTE_GROUP, new String[] {"gid2"});
			defs.put(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, new String[] {"gid1", "gid4"});
			ret.setAllIncarnationAttributes(defs, valid);
			ret.setPreferredVoIncarnationAttributes("/vo2", new HashMap<String, String[]>());
			return ret;
		}

		@Override
		public String getStatusDescription() {
			return "OK";
		}

		@Override
		public String getName() {
			return "MOCK-AIP";
		}

		@Override
		public void configure(String name) throws ConfigurationException {
		}

		@Override
		public void start(Kernel kernel) throws Exception {
		}
	}
}
