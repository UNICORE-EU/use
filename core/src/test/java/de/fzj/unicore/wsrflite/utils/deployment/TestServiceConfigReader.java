package de.fzj.unicore.wsrflite.utils.deployment;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Service;
import de.fzj.unicore.wsrflite.testservice.MockService;

public class TestServiceConfigReader extends TestCase {

	public void testDeployService()throws Exception{
		Kernel k=new Kernel("src/test/resources/conf/use.properties");
		k.start();
		Service s=k.getService("test");
		assertNotNull(s);
		assertEquals(MockService.TYPE,s.getType());
	}
	
	/**
	 * tests that the "uas.onstartup*" properties are properly processed
	 */
	public void testStartupTasks()throws Exception{
		String config = "src/test/resources/conf/use-2.properties";
		Kernel k=new Kernel(config);
		ServiceConfigReader r=new ServiceConfigReader(k, new File(config));
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
