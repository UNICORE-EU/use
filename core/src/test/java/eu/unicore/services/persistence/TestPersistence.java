package eu.unicore.services.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.H2Persist;
import eu.unicore.persist.impl.InMemory;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Model;
import eu.unicore.services.impl.BaseModel;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.util.ConcurrentAccess;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;

public class TestPersistence {

	@Test
	public void testLockSupport() throws Exception{
		Kernel k = new Kernel(TestConfigUtil.getInsecureProperties());
		LockSupport ls=k.getPersistenceManager().getLockSupport();
		assertNotNull(ls);
	}

	@Test
	public void testStoreHandling() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		PersistenceManager p=k.getPersistenceManager();
		Store s=p.getPersist("test");
		assertNotNull(s);
		Store s2=p.getPersist("test");
		assertNotNull(s2);
		assertTrue(s==s2);

		p.removePersist("test");
		s=p.persistMap.get("test");
		assertNull(s);
	}

	@Test
	public void testGetPersistenceSettings() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		PersistenceManager p=k.getPersistenceManager();
		PersistenceSettings ps=p.getPersistenceSettings(ResourceImpl.class);
		assertNotNull(ps);
	}

	@Test
	public void testInMemory() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		PersistenceManager p=k.getPersistenceManager();
		k.getPersistenceProperties().setProperty("class.test", InMemory.class.getName());
		Store s = p.getPersist("test");
		assertTrue(s instanceof Persistence);
		assertTrue (((Persistence)s).getBackEnd() instanceof InMemory);
		assertNotNull(s.getUniqueIDs());
	}

	@Test
	public void testBasicPersistence()throws Exception{
		File tmp=new File("target","testdata");
		FileUtils.deleteQuietly(tmp);
		PersistenceProperties configSource=new PersistenceProperties();
		configSource.setProperty(PersistenceProperties.DB_DIRECTORY, "target/testdata/resource-bean-test");
		configSource.setProperty(PersistenceProperties.DB_IMPL, H2Persist.class.getName());
		Persist<ResourceBean>p=PersistenceFactory.get(configSource).getPersist(ResourceBean.class);		
		
		MyModel m = new MyModel();
		m.setUniqueID("123");
		AssertionDocument ad = AssertionDocument.Factory.newInstance();
		ad.addNewAssertion().addNewIssuer().setStringValue("http://test");
		m.setTest(ad);
		
		ResourceBean rb = new ResourceBean("123", "test123", ResourceImpl.class.getName(), m);
		p.write(rb);
		
		ResourceBean rb2 = p.read("123"); 
		assertNotNull(rb2);
		Model m2 = rb2.getState();
		assertNotNull(m2);
		assertEquals("123",m2.getUniqueID());
		p.shutdown();
		FileUtils.deleteQuietly(tmp);
	}

	@Test
	public void testTags()throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		File tmp=new File("target","testdata");
		Persistence p = (Persistence)k.getPersistenceManager().getPersist("test");
		assertNotNull(p);		

		String[] tags = new String[]{"tag1", "tag2"};
		for(String tag: tags){
			for(int i=0;i<5;i++){
				ResourceImpl m1 = new ResourceImpl(){};
				m1.setKernel(k);
				m1.initialise(new InitParameters("r_"+tag+"_"+i));
				BaseModel m = m1.getModel();
				m.getTags().add("other");
				m.getTags().add(tag);
				m.getTags().add("foo");
				p.persist(m1);
			}
		}
		assertEquals(10, p.getBackEnd().getRowCount());
		
		assertEquals(5, p.getTaggedResources("tag1").size());
		List<String>tagged2 = p.getTaggedResources("tag2");
		assertEquals(5, tagged2.size());
		for(String id : tagged2){
			assertTrue(id.contains("_tag2_"));
		}
		List<String>tagged3 = p.getTaggedResources("tag2", "other");
		assertEquals(5, tagged3.size());
		for(String id : tagged3){
			assertTrue(id.contains("_tag2_"));
		}

		List<String>tagged4 = p.getTaggedResources("nope", "tag1");
		assertEquals(0, tagged4.size());

		p.shutdown();
		FileUtils.deleteQuietly(tmp);
	}

	public static class MyModel extends BaseModel{

		private static final long serialVersionUID = 1L;

		private AssertionDocument test;

		public AssertionDocument getTest() {
			return test;
		}

		public void setTest(AssertionDocument test) {
			this.test = test;
		}
	}
	

	@Test
	public void testAnnotations(){
		PersistenceSettings ps=PersistenceSettings.get(MockWSR1.class);
		assertTrue(ps.isConcurrentMethod("foo"));
		assertFalse(ps.isConcurrentMethod("bar"));
	}

	@Test
	public void testAnnotationsInherited(){
		PersistenceSettings ps=PersistenceSettings.get(MockWSR2.class);
		assertTrue(ps.isConcurrentMethod("foo"));
		assertTrue(ps.isConcurrentMethod("bar"));
	}

	@Test
	public void testSpecifyConcurrentMethods(){
		PersistenceSettings ps=PersistenceSettings.get(MockWSR1.class);
		List<String>methods=ps.getConcurrentMethodNames();
		assertTrue(methods.contains("foo"));
		assertFalse(methods.contains("bar"));
		assertFalse(methods.contains("baz"));
	}

	public class MockWSR1{
		@ConcurrentAccess(allow=true)
		public void foo(){}
		@ConcurrentAccess
		public void bar(){}
		public void baz(){}
	}

	public class MockWSR2 extends MockWSR1{
		@ConcurrentAccess(allow=true)
		public void bar(){}
	}
}
