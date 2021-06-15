package eu.unicore.services.security;

import static eu.unicore.services.security.ContainerSecurityProperties.PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_COMBINING_POLICY;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_ORDER;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_CHECKACCESS;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_DAP_ORDER;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_DAP_PREFIX;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.Xlogin;
import eu.unicore.security.canl.DefaultAuthnAndTrustConfiguration;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AttributeSourcesChain;
import eu.unicore.util.configuration.ConfigurationException;
import junit.framework.TestCase;

public class TestSecurityManager extends TestCase {
	private static int calls=0;
	private static boolean aInitCalled = false;
	private static int intProperty;
	private static boolean booleanProperty;
	private static String stringProperty = "";
	private static String name = "";

	private static int dcalls=0;
	private static boolean daInitCalled = false;
	private static int dintProperty;
	private static boolean dbooleanProperty;
	private static String dstringProperty = "";
	private static String dname = "";

	public void testAuthZChainNew()throws Exception{
		calls=0;
		aInitCalled = false;
		intProperty = 0;
		booleanProperty = false;
		stringProperty = "";
		name = "";
		dcalls=0;
		daInitCalled = false;
		dintProperty = 0;
		dbooleanProperty = false;
		dstringProperty = "";
		dname = "";
		Properties p = new Properties();
		p.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.class", 
				SimpleAIP.class.getName());
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.class", 
				SimpleAIP.class.getName());
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.intProperty", "8");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.intProperty", "8");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.booleanProperty", "true");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.booleanProperty", "true");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.stringProperty", "test");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.stringProperty", "test");
		p.setProperty(PREFIX+PROP_AIP_ORDER, "A1 A2");
		
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A1.class", 
				SimpleDAP.class.getName());
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A2.class", 
				SimpleDAP.class.getName());
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A1.intProperty", "8");
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A2.intProperty", "8");
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A1.booleanProperty", "true");
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A2.booleanProperty", "true");
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A1.stringProperty", "test");
		p.setProperty(PREFIX+PROP_DAP_PREFIX+"."+"A2.stringProperty", "test");
		p.setProperty(PREFIX+PROP_DAP_ORDER, "A1 A2");
		
		
		ContainerSecurityProperties secProps = new ContainerSecurityProperties(p,
				new DefaultAuthnAndTrustConfiguration());
		Kernel k = new Kernel(TestConfigUtil.getInsecureProperties());
		secProps.getAip().start(k);
		secProps.getDap().start(k);
		SecurityManager secMan = new SecurityManager(secProps);
		
		SecurityTokens secTokens = new SecurityTokens();
		SubjectAttributesHolder res = secMan.establishAttributes(secTokens);
		Client client = new Client();
		client.setAuthenticatedClient(secTokens);
		client.setXlogin(new Xlogin(new String[]{}, new String[] {"staticGroup"}));
		secMan.collectDynamicAttributes(client);
		assertNotNull(res);
		assertEquals(2, calls);
		assertTrue(aInitCalled);
		assertTrue(intProperty == 16);
		assertTrue(booleanProperty);
		assertTrue(stringProperty.equals("testtest"));
		assertTrue("A1A2".equals(name));

		assertEquals(2, dcalls);
		assertTrue(daInitCalled);
		assertTrue(dintProperty == 16);
		assertTrue(dbooleanProperty);
		assertTrue(dstringProperty, dstringProperty.equals("testtest"));
		assertTrue("A1A2".equals(dname));
		
		assertEquals("dynamicUid", client.getSelectedXloginName());
		assertEquals("dynamicGid", client.getXlogin().getGroup());
	
	}

	public void testAuthZChainNew2()throws Exception{
		calls=0;
		aInitCalled = false;
		intProperty = 0;
		booleanProperty = false;
		stringProperty = "";
		name = "";
		Properties p = new Properties();
		p.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.class", 
				SimpleAIP.class.getName());
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.class", 
				SimpleAIP.class.getName());
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.intProperty",	"8");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.intProperty", "8");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.booleanProperty", "true");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.booleanProperty", "true");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A1.stringProperty", "test");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+"."+"A2.stringProperty", "test");

		p.setProperty(PREFIX+PROP_AIP_ORDER, "A1 A2");
		p.setProperty(PREFIX+PROP_AIP_COMBINING_POLICY, 
				AttributeSourcesChain.FirstAccessible.NAME);
		ContainerSecurityProperties secProps = new ContainerSecurityProperties(p,
				new DefaultAuthnAndTrustConfiguration());
		secProps.getAip().start(new Kernel(TestConfigUtil.getInsecureProperties()));
		SecurityManager secMan = new SecurityManager(secProps);
		SubjectAttributesHolder res = secMan.establishAttributes(new SecurityTokens());
		assertNotNull(res);
		assertTrue(aInitCalled);
		assertTrue(intProperty == 16);
		assertTrue(booleanProperty);
		assertTrue(stringProperty.equals("testtest"));
		assertTrue("A1A2".equals(name));
		
	}


	public static class SimpleAIP implements IAttributeSource {
		@Override
		public void configure(String n) throws ConfigurationException {
			name += n;
		}

		@Override
		public void start(Kernel kernel) throws Exception {
			aInitCalled = true;
		}

		public String getStatusDescription(){
			return "OK";
		}
		
		public void setIntProperty(int v) {
			intProperty += v;
		}

		public void setStringProperty(String v) {
			stringProperty = stringProperty + v;
		}

		public void setBooleanProperty(boolean v) {
			booleanProperty = v;
		}

		@Override
		public String getName()	{
			return null;
		}

		@Override
		public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
				SubjectAttributesHolder otherAuthoriserInfo)
				throws IOException {
			Map<String,String[]>res=new HashMap<String,String[]>();
			res.put(IAttributeSource.ATTRIBUTE_ROLE, new String[]{"user"});
			calls++;
			return new SubjectAttributesHolder(res);
		}
	
	}

	public static class SimpleDAP implements IDynamicAttributeSource {
		@Override
		public void configure(String n) throws ConfigurationException {
			dname += n;
		}

		@Override
		public void start(Kernel kernel) throws Exception {
			daInitCalled = true;
		}

		public String getStatusDescription(){
			return "OK";
		}
		
		public void setIntProperty(int v) {
			dintProperty += v;
		}

		public void setStringProperty(String v) {
			dstringProperty = dstringProperty + v;
		}

		public void setBooleanProperty(boolean v) {
			dbooleanProperty = v;
		}

		@Override
		public String getName()	{
			return null;
		}

		@Override
		public SubjectAttributesHolder getAttributes(Client client,
				SubjectAttributesHolder otherAuthoriserInfo)
				throws IOException {
			Map<String,String[]>res=new HashMap<String,String[]>();
			if (dcalls == 0)
			{
				res.put(IAttributeSource.ATTRIBUTE_GROUP, new String[]{"dynamicGid"});
			} else
			{
				res.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{"dynamicUid"});
			}
			dcalls++;
			return new SubjectAttributesHolder(res);
		}
	}
}
