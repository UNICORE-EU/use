package de.fzj.unicore.wsrflite.persistence;

import java.io.File;
import java.util.List;

import org.junit.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import de.fzj.unicore.persist.Persist;
import de.fzj.unicore.persist.PersistenceFactory;
import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.persist.impl.H2Persist;
import de.fzj.unicore.wsrflite.Model;
import de.fzj.unicore.wsrflite.impl.BaseModel;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;

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
		Assert.assertEquals("2", b2.getSubscriberCount());
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
		Assert.assertNotNull(rb2);
		Model m2 = rb2.getState();
		Assert.assertNotNull(m2);
		Assert.assertEquals("123",m2.getUniqueID());
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
		Assert.assertEquals(10, p.getRowCount());
		
		Assert.assertEquals(5, p.findIDs("tags", "tag1").size());
		List<String>tagged2 = p.findIDs("tags", "tag2");
		Assert.assertEquals(5, tagged2.size());
		for(String id : tagged2){
			Assert.assertTrue(id.contains("_tag2_"));
		}
		List<String>tagged3 = p.findIDs("tags", "tag2", "other");
		Assert.assertEquals(5, tagged3.size());
		for(String id : tagged3){
			Assert.assertTrue(id.contains("_tag2_"));
		}
		
		List<String>tagged4 = p.findIDs("tags", "nope", "tag1");
		Assert.assertEquals(0, tagged4.size());
		
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
