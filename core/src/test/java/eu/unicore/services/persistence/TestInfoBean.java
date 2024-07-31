package eu.unicore.services.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.H2Persist;
import eu.unicore.services.Model;
import eu.unicore.services.impl.BaseModel;
import eu.unicore.services.impl.ResourceImpl;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;

public class TestInfoBean {
	
	@Test
	public void test1()throws Exception{
		File tmp=new File("target","testdata");
		FileUtils.deleteQuietly(tmp);
		PersistenceProperties configSource=new PersistenceProperties();
		configSource.setProperty(PersistenceProperties.DB_DIRECTORY, "target/testdata/info-bean-test");
		configSource.setProperty(PersistenceProperties.DB_IMPL, H2Persist.class.getName());
		Persist<InstanceInfoBean>p=PersistenceFactory.get(configSource).getPersist(InstanceInfoBean.class);		
		InstanceInfoBean b1=new InstanceInfoBean("123","test",null);
		b1.incrementSubscriberCount();
		b1.incrementSubscriberCount();
		p.write(b1);
		InstanceInfoBean b2=p.read("123");
		assertEquals("2", b2.getSubscriberCount());
		p.shutdown();
		FileUtils.deleteQuietly(tmp);
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
		File tmp=new File("target","testdata");
		FileUtils.deleteQuietly(tmp);
		PersistenceProperties configSource=new PersistenceProperties();
		configSource.setProperty(PersistenceProperties.DB_DIRECTORY, "target/testdata/resource-bean-test");
		configSource.setProperty(PersistenceProperties.DB_IMPL, H2Persist.class.getName());
		Persist<ResourceBean>p=PersistenceFactory.get(configSource).getPersist(ResourceBean.class);		
		
		String[] tags = new String[]{"tag1", "tag2"};
		for(String tag: tags){
			for(int i=0;i<5;i++){
				BaseModel m = new BaseModel();
				m.setUniqueID("r_"+tag+"_"+i);
				m.getTags().add("other");
				m.getTags().add(tag);
				m.getTags().add("foo");
				ResourceBean rb = new ResourceBean(m.getUniqueID(), "test123", ResourceImpl.class.getName(), m);
				p.write(rb);
			}
		}
		assertEquals(10, p.getRowCount());
		
		assertEquals(5, p.findIDs("tags", "tag1").size());
		List<String>tagged2 = p.findIDs("tags", "tag2");
		assertEquals(5, tagged2.size());
		for(String id : tagged2){
			assertTrue(id.contains("_tag2_"));
		}
		List<String>tagged3 = p.findIDs("tags", "tag2", "other");
		assertEquals(5, tagged3.size());
		for(String id : tagged3){
			assertTrue(id.contains("_tag2_"));
		}
		
		List<String>tagged4 = p.findIDs("tags", "nope", "tag1");
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
}
