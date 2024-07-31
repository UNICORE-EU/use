package eu.unicore.services.utils.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.testservice.MockService;

public class TestServiceConfigReader {

	@Test
	public void testDeployService()throws Exception{
		Kernel k=new Kernel("src/test/resources/conf/use.properties");
		k.start();
		Service s=k.getService("test");
		assertNotNull(s);
		assertEquals(MockService.TYPE,s.getType());
	}

	@Test
	public void testStartupTasks()throws Exception{
		String config = "src/test/resources/conf/use-2.properties";
		Kernel k=new Kernel(config);
		ServiceConfigurator r=new ServiceConfigurator(k, new File(config));
		r.loadProperties();
		r.configureServices();
		List<Runnable>tasks=r.getInitTasks();
		assertNotNull(tasks);
		assertEquals(2, tasks.size());
		//check ordering
		assertTrue(tasks.get(1).getClass().getName().endsWith("2"));
	}
	
	public static class MockStartup implements Runnable{
		public void run(){}
	}
	
	public static class MockStartup2 implements Runnable{
		public void run(){}
	}

}
