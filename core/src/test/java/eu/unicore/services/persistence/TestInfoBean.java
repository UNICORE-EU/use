package eu.unicore.services.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.H2Persist;

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

}
