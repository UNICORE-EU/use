package eu.unicore.services.utils.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.StartupTask;
import eu.unicore.services.testservice.MockService;

public class TestServiceConfigurator {

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
		List<StartupTask>tasks=r.getInitTasks();
		assertNotNull(tasks);
		// 2 tasks from features + 2 tasks from config file 
		assertEquals(4, tasks.size());
	}
	
	public static class MockStartup implements StartupTask {
		public void run(){}
	}
	
	public static class MockStartup2 implements StartupTask {
		public void run(){}
	}

	@Test
	public void testStartupTaskRunner()throws Exception{
		Kernel k = new Kernel("src/test/resources/conf/use.properties");
		List<StartupTask> tasks = new ArrayList<>();
		tasks.add(new T1());
		tasks.add(new T2());
		tasks.add(new T3());
		StartupTasksRunner r = new StartupTasksRunner(k, tasks);
		List<StartupTask> ordered = r.getOrderedTasks();
		assertEquals("T2", ordered.get(0).getName());
		assertEquals("T1", ordered.get(1).getName());
		assertEquals("T3", ordered.get(2).getName());	
	}

	public static class T1 implements StartupTask {
		public String getName() {return "T1";}
		public void run(){}
	}

	public static class T2 implements StartupTask {
		public Collection<String>getBefore(){
			return Arrays.asList("T1");
		}
		public String getName() {return "T2";}
		public void run(){}
	}

	public static class T3 implements StartupTask {
		public Collection<String>getAfter(){
			return Arrays.asList("T1");
		}
		public String getName() {return "T3";}
		public void run(){}
	}
}
