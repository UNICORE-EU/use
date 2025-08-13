package eu.unicore.services.security;

import static eu.unicore.services.security.ContainerSecurityProperties.PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_COMBINING_POLICY;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_ORDER;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_CHECKACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.security.XACMLAttribute.Type;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AttributeSourceConfigurator;
import eu.unicore.services.security.util.AttributeSourcesChain;
import eu.unicore.services.security.util.BaseAttributeSourcesChain.CombiningPolicy;
import eu.unicore.util.configuration.ConfigurationException;

public class TestAuthChainFactory {

	@Test
	public void testNoConfigCase()throws Exception{
		Properties props = getDefaultConfig();
		props.setProperty("container.security.attributes.order", "");
		Kernel k = new Kernel(props);
		IAttributeSource a = k.getSecurityManager().getAip();
		assertNotNull(a);
		System.out.println(a);
		assertTrue(a instanceof AttributeSourcesChain);
		assertTrue(((AttributeSourcesChain)a).getChain().get(0)
				instanceof AuthAttributesCollector);
	}

	@Test
	public void testConfigureAuthoriser()throws Exception{
		Properties props = new Properties();
		props.put(PREFIX+PROP_AIP_ORDER,"TEST1");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.class",TestAuthZ.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.Foo","FooParam");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.Bar","1234");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.Flag","true");
		IAttributeSourceBase ab=AttributeSourceConfigurator.configureAS("TEST1", PROP_AIP_PREFIX, props);
		assertNotNull(ab);
		IAttributeSource a = (IAttributeSource)ab;
		assertTrue(a instanceof TestAuthZ);
		TestAuthZ ta=(TestAuthZ)a;
		assertEquals("FooParam",ta.getFoo());
		assertEquals(1234,ta.getBar());
		assertTrue(ta.isFlag());
	}

	@Test
	public void testChain()throws Exception{
		Properties props = getDefaultConfig();
		props.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		props.put(PREFIX+PROP_AIP_ORDER,"TEST2 TEST1");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.class",TestAuthZ.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST2.class",TestAuthZ2.class.getName());
		Kernel k = new Kernel(props);
		IAttributeSource a = k.getSecurityManager().getAip();
		assertNotNull(a);
		assertTrue(a instanceof AttributeSourcesChain);
		AttributeSourcesChain chain=(AttributeSourcesChain)a;
		
		List<IAttributeSource>authZChain=chain.getChain();
		assertEquals(3, authZChain.size());
		System.out.println(authZChain);
		assertTrue(authZChain.get(1) instanceof TestAuthZ2);
		assertTrue(authZChain.get(2) instanceof TestAuthZ);
	}

	@Test
	public void testChainCombiningPolicy()throws Exception{
		Properties props = getDefaultConfig();
		props.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		props.put(PREFIX+PROP_AIP_ORDER,"TEST TEST1");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.class",TestAuthZ.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST.class",TestAuthZ2.class.getName());
		props.put(PREFIX+PROP_AIP_COMBINING_POLICY, "FIRST_APPLICABLE");
		Kernel k = new Kernel(props);
		IAttributeSource a = k.getSecurityManager().getAip();
		assertNotNull(a);
		assertTrue(a instanceof AttributeSourcesChain);

		AttributeSourcesChain chain=(AttributeSourcesChain)a;
		List<IAttributeSource>authZChain=chain.getChain();
		assertEquals(3, authZChain.size());
		assertTrue(authZChain.get(0) instanceof TestAuthZ2);
		assertTrue(authZChain.get(1) instanceof TestAuthZ);
		assertTrue(chain.getCombiningPolicy() == AttributeSourcesChain.FIRST_APPLICABLE);
	}

	@Test
	public void testChainCustomCombiningPolicy()throws Exception{
		Properties props = getDefaultConfig();
		props.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		props.put(PREFIX+PROP_AIP_ORDER,"TEST2 TEST1");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.class",TestAuthZ.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST2.class",TestAuthZ2.class.getName());
		props.put(PREFIX+PROP_AIP_COMBINING_POLICY,TestPolicy.class.getName());
		Kernel k = new Kernel(props);
		IAttributeSource a = k.getSecurityManager().getAip();
		
		assertNotNull(a);
		assertTrue(a instanceof AttributeSourcesChain);

		AttributeSourcesChain chain=(AttributeSourcesChain)a;
		List<IAttributeSource>authZChain=chain.getChain();
		assertEquals(3, authZChain.size());
		assertTrue(authZChain.get(0) instanceof TestAuthZ2);
		assertTrue(authZChain.get(1) instanceof TestAuthZ);
		assertTrue(chain.getCombiningPolicy() instanceof TestPolicy);
	}

	@Test
	public void testMergePolicy(){
		CombiningPolicy m = AttributeSourcesChain.MERGE;
		String[] a1=new String[]{"foo","bar"};
		String[] a2=new String[]{"spam","ham"};
		Map<String,String[]>master = new HashMap<>();
		Map<String,String[]>newAttributes = new HashMap<>();
		master.put("test", a1);
		newAttributes.put("test", a2);

		List<XACMLAttribute> xacmlAttributes1 = new ArrayList<>();
		xacmlAttributes1.add(new XACMLAttribute("a1", "v1", Type.STRING));
		xacmlAttributes1.add(new XACMLAttribute("a1", "http://host", Type.ANYURI));
		xacmlAttributes1.add(new XACMLAttribute("a2", "v1", Type.STRING));
		xacmlAttributes1.add(new XACMLAttribute("a2", "v4", Type.STRING));

		List<XACMLAttribute> xacmlAttributes2 = new ArrayList<>();
		xacmlAttributes2.add(new XACMLAttribute("a1", "v1", Type.STRING));
		xacmlAttributes2.add(new XACMLAttribute("a1", "http://host2", Type.ANYURI));
		xacmlAttributes2.add(new XACMLAttribute("a2", "v5", Type.STRING));
		xacmlAttributes2.add(new XACMLAttribute("a3", "v1", Type.STRING));

		SubjectAttributesHolder masterH = new SubjectAttributesHolder(master, master);
		masterH.setXacmlAttributes(xacmlAttributes1);
		SubjectAttributesHolder newH = new SubjectAttributesHolder(newAttributes, newAttributes);
		newH.setXacmlAttributes(xacmlAttributes2);

		m.combineAttributes(masterH, newH);
		String[]x=masterH.getValidIncarnationAttributes().get("test");
		assertEquals(4,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertTrue(s.contains("bar"));
		assertTrue(s.contains("foo"));
		assertTrue(s.contains("ham"));
		assertTrue(s.contains("spam"));

		String[]x2=masterH.getDefaultIncarnationAttributes().get("test");
		assertEquals(2,x2.length);
		String s2=String.valueOf(Arrays.asList(x2));
		assertTrue(s2.contains("ham"));
		assertTrue(s2.contains("spam"));

		List<XACMLAttribute> x3 = masterH.getXacmlAttributes();
		assertEquals(7, x3.size());
		assertTrue(x3.contains(xacmlAttributes1.get(0)));
		assertTrue(x3.contains(xacmlAttributes1.get(1)));
		assertTrue(x3.contains(xacmlAttributes1.get(2)));
		assertTrue(x3.contains(xacmlAttributes1.get(3)));
		assertTrue(x3.contains(xacmlAttributes2.get(1)));
		assertTrue(x3.contains(xacmlAttributes2.get(2)));
		assertTrue(x3.contains(xacmlAttributes2.get(3)));
	}

	@Test
	public void testMergeOverridesPolicy(){
		CombiningPolicy m = AttributeSourcesChain.MERGE_LAST_OVERRIDES;
		String[] a1=new String[]{"foo","bar"};
		String[] a2=new String[]{"spam","ham"};
		Map<String,String[]>master = new HashMap<>();
		Map<String,String[]>newAttributes = new HashMap<>();
		master.put("test", a1);
		newAttributes.put("test", a2);
		SubjectAttributesHolder masterH = new SubjectAttributesHolder(master, master);
		SubjectAttributesHolder newH = new SubjectAttributesHolder(newAttributes, newAttributes);

		m.combineAttributes(masterH, newH);
		String[]x=masterH.getValidIncarnationAttributes().get("test");
		assertEquals(2,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertFalse(s.contains("bar"));
		assertFalse(s.contains("foo"));
		assertTrue(s.contains("ham"));
		assertTrue(s.contains("spam"));
	}

	@Test
	public void testFirstApplicablePolicy(){
		CombiningPolicy m = AttributeSourcesChain.FIRST_APPLICABLE;
		String[] a1=new String[]{"foo","bar"};
		String[] a2=new String[]{"spam","ham"};
		Map<String,String[]>master = new HashMap<>();
		Map<String,String[]>newAttributes = new HashMap<>();
		master.put("test", a1);
		newAttributes.put("test", a2);
		SubjectAttributesHolder masterH = new SubjectAttributesHolder(master, master);
		SubjectAttributesHolder newH = new SubjectAttributesHolder(newAttributes, newAttributes);

		m.combineAttributes(masterH, newH);
		String[]x=masterH.getValidIncarnationAttributes().get("test");
		assertEquals(2,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertTrue(s.contains("bar"));
		assertTrue(s.contains("foo"));
		assertFalse(s.contains("ham"));
		assertFalse(s.contains("spam"));
	}

	@Test
	public void testFirstAccessiblePolicy() throws Exception {
		Properties props = getDefaultConfig();
		props.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		props.put(PREFIX+PROP_AIP_ORDER,"TEST2 TEST1 TEST3");
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST1.class",TestAuthZError.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST2.class",TestAuthZSingle1.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST3.class",TestAuthZSingle2.class.getName());
		props.put(PREFIX+PROP_AIP_COMBINING_POLICY, "FIRST_ACCESSIBLE");
		Kernel k = new Kernel(props);
		IAttributeSource chain = k.getSecurityManager().getAip();
		
		SubjectAttributesHolder newAttributes;
		newAttributes = chain.getAttributes(new SecurityTokens(), new SubjectAttributesHolder());
		String[]x = newAttributes.getValidIncarnationAttributes().get("test");
		assertEquals(2,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertTrue(s.contains("bar"));
		assertTrue(s.contains("foo"));
		assertFalse(s.contains("ham"));
		assertFalse(s.contains("spam"));
	}

	private Properties prepareProperties() {
		Properties props = getDefaultConfig();
		props.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		props.put(PREFIX+PROP_AIP_ORDER,"CHAIN TEST");
		props.put(PREFIX+PROP_AIP_COMBINING_POLICY, "MERGE_LAST_OVERRIDES");
		props.put(PREFIX+PROP_AIP_PREFIX+".CHAIN.class", 
				AttributeSourcesChain.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".CHAIN.combiningPolicy", "FIRST_ACCESSIBLE");
		props.put(PREFIX+PROP_AIP_PREFIX+".CHAIN.order", 
				"SUB-T1 SUB-T2");
		return props;
	}

	@Test
	public void testChainedPolicy1() throws Exception {
		Properties props=prepareProperties();
		addDefaultConfig(props);
		props.put(PREFIX+PROP_AIP_PREFIX+".SUB-T1.class",
				TestAuthZError.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".SUB-T2.class",
				TestAuthZSingle2.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST.class", 
				TestAuthZSingle1.class.getName());
		Kernel k = new Kernel(props);
		IAttributeSource chain = k.getSecurityManager().getAip();
		
		SubjectAttributesHolder newAttributes = chain.getAttributes(
				new SecurityTokens(), new SubjectAttributesHolder());
		String[]x=newAttributes.getValidIncarnationAttributes().get("test");
		assertTrue(x != null);
		assertEquals(2,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertTrue(s.contains("bar"));
		assertTrue(s.contains("foo"));
		assertFalse(s.contains("ham"));
		assertFalse(s.contains("spam"));
	}

	@Test
	public void testChainedPolicy2() throws Exception {
		Properties props=prepareProperties();
		addDefaultConfig(props);
		props.put(PREFIX+PROP_AIP_PREFIX+".SUB-T1.class",
				TestAuthZSingle1.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".SUB-T2.class",
				TestAuthZSingle2.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST.class", 
				TestAuthZError.class.getName());
		Kernel k = new Kernel(props);
		IAttributeSource chain = k.getSecurityManager().getAip();
		
		SubjectAttributesHolder newAttributes = chain.getAttributes(
				new SecurityTokens(), new SubjectAttributesHolder());
		String[]x=newAttributes.getValidIncarnationAttributes().get("test");
		assertTrue(x != null);
		assertEquals(2,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertTrue(s.contains("bar"));
		assertTrue(s.contains("foo"));
		assertFalse(s.contains("ham"));
		assertFalse(s.contains("spam"));
	}

	@Test
	public void testChainedPolicy3() throws Exception {
		Properties props=prepareProperties();
		addDefaultConfig(props);
		props.put(PREFIX+PROP_AIP_PREFIX+".SUB-T1.class",
				TestAuthZSingle1.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".SUB-T2.class",
				TestAuthZSingle2.class.getName());
		props.put(PREFIX+PROP_AIP_PREFIX+".TEST.class", 
				TestAuthZSingle3.class.getName());

		Kernel k = new Kernel(props);
		IAttributeSource chain=k.getSecurityManager().getAip();
		
		SubjectAttributesHolder newAttributes = chain.getAttributes(
				new SecurityTokens(), new SubjectAttributesHolder());
		String[]x=newAttributes.getValidIncarnationAttributes().get("test");
		assertTrue(x != null);
		assertEquals(2,x.length);
		String s=String.valueOf(Arrays.asList(x));
		assertTrue(s.contains("bar"));
		assertTrue(s.contains("foo"));
		assertFalse(s.contains("ham"));
		assertFalse(s.contains("spam"));
		String[]x2=newAttributes.getValidIncarnationAttributes().get("testA");
		assertTrue(x2 != null);
		assertEquals(2,x2.length);
		String s2=String.valueOf(Arrays.asList(x2));
		assertTrue(s2.contains("ham"));
		assertTrue(s2.contains("spam"));
		assertFalse(s2.contains("foo"));
		assertFalse(s2.contains("bar"));
	}


	//test class
	public static class TestAuthZ implements IAttributeSource{
		private String foo;
		private int bar;
		private boolean flag;

		public boolean isFlag() {
			return flag;
		}

		public void setFlag(boolean flag) {
			this.flag = flag;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public int getBar() {
			return bar;
		}

		public void setBar(int bar) {
			this.bar = bar;
		}

		@Override
		public void configure(String name, Kernel k) throws ConfigurationException {}

		public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder unused)	throws IOException, AuthorisationException {
			return new SubjectAttributesHolder();
		}

		public String getName()	{
			return "TestAuthZ";
		}

	}

	//and another one
	public static class TestAuthZ2 implements IAttributeSource{
		@Override
		public void configure(String name, Kernel k) throws ConfigurationException {}

		public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder unused)	throws IOException, AuthorisationException {
			return new SubjectAttributesHolder();
		}

		public String getName()	{
			return "TestAuthZ2";
		}

	}

	public static class TestAuthZError implements IAttributeSource{

		@Override
		public void configure(String name, Kernel k) throws ConfigurationException {}

		public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder unused)	throws IOException, AuthorisationException {
			throw new IOException();
		}

		public String getName()	{
			return "TestAuthZError";
		}

	}

	public static class TestAuthZSingle1 implements IAttributeSource{

		@Override
		public void configure(String name, Kernel k) throws ConfigurationException {}

		public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder unused)	throws IOException, AuthorisationException {
			Map<String, String[]> ret = new HashMap<String, String[]>();
			ret.put("test", new String[] {"foo", "bar"});
			return new SubjectAttributesHolder(ret, ret);
		}

		public String getName()	{
			return "TestAuthZSingle1";
		}

	}

	public static class TestAuthZSingle2 implements IAttributeSource{
		@Override
		public void configure(String name, Kernel k) throws ConfigurationException {}

		public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder unused)
				throws IOException, AuthorisationException {
			Map<String, String[]> ret = new HashMap<String, String[]>();
			ret.put("test", new String[] {"ham", "spam"});
			return new SubjectAttributesHolder(ret, ret);
		}

		public String getName()	{
			return "TestAuthZSingle2";
		}
	}

	public static class TestAuthZSingle3 implements IAttributeSource{

		public SubjectAttributesHolder getAttributes(SecurityTokens tokens,SubjectAttributesHolder unused)
				throws IOException, AuthorisationException {
			Map<String, String[]> ret = new HashMap<String, String[]>();
			ret.put("testA", new String[] {"ham", "spam"});
			return new SubjectAttributesHolder(ret, ret);
		}

		public String getName()	{
			return "TestAuthZSingle3";
		}

		@Override
		public void configure(String name, Kernel k) throws ConfigurationException {}
	}

	//mock policy
	public static class TestPolicy implements CombiningPolicy{
		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes) {
			return true;
		}
		public String toString(){return "TEST";}
	}

	private Properties getDefaultConfig() {
		Properties props = TestConfigUtil.getInsecureProperties();
		addDefaultConfig(props);
		return props;
	}

	private void addDefaultConfig(Properties props) {
		props.put("container.security.sslEnabled", "false");
		props.put("container.security.gateway.enable", "false");
	}
}
