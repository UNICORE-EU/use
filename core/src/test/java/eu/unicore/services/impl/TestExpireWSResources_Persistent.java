package eu.unicore.services.impl;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import eu.unicore.persist.PersistenceProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.persistence.Persistence;
import eu.unicore.services.persistence.Store;
import eu.unicore.services.security.TestConfigUtil;

public class TestExpireWSResources_Persistent extends TestExpireWSResources{

	private static String dir="target/data-test-expiry";
	
	@Before
	public void setUp()throws Exception{
		FileUtils.deleteQuietly(new File(dir));
		super.setUp();
	}
	
	@After
	public void tearDown(){
		FileUtils.deleteQuietly(new File(dir));
	}
	
	@Override
	protected Store createStore()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(PersistenceProperties.PREFIX + PersistenceProperties.DB_DIRECTORY, dir);
		Kernel k=new Kernel(p);
		Store s=new Persistence();
		s.init(k,"test123");
		return s;
	}
	
}
