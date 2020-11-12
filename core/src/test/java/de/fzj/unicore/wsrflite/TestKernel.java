package de.fzj.unicore.wsrflite;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.server.AbstractStartupTask;
import de.fzj.unicore.wsrflite.server.GatewayHandler;
import de.fzj.unicore.wsrflite.server.StartupTask;
import de.fzj.unicore.wsrflite.server.StartupTasksRunner;
import de.fzj.unicore.wsrflite.testservice.MockHome;
import de.fzj.unicore.wsrflite.utils.deployment.DemoFeature;
import de.fzj.unicore.wsrflite.utils.deployment.DemoFeature2;
import de.fzj.unicore.wsrflite.utils.deployment.DemoFeature3;
import junit.framework.TestCase;

public class TestKernel extends TestCase {

	protected void tearDown(){
		ranStartupTask1=false;
		ranStartupTask2=false;
	}

	public void testHeader() throws Exception{	
		System.out.println(new Kernel(TestConfigUtil.getInsecureProperties()).getHeader());
	}

	public void testKernelStartup()throws Exception{
		Kernel k=new Kernel("src/test/resources/conf/use.properties");
		try{
			assertNotNull(k);
			k.startSynchronous();
			IContainerSecurityConfiguration sp = k.getContainerSecurityConfiguration();
			assertNotNull(sp);
			GatewayHandler gw=k.getGatewayHandler();
			assertNotNull(gw);
			String status=gw.getConnectionStatusMessage();
			System.out.println(status);
			assertNotNull(status);
			assertTrue(status.contains("N/A"));
			assertEquals(ExternalSystemConnector.Status.NOT_APPLICABLE,gw.getConnectionStatus());

			Service mock=k.getService("test");
			assertNotNull(mock);
			assertTrue(MockHome.startupTaskWasRun);
			assertTrue(DemoFeature.initWasRun);
			assertTrue(k.getDeploymentManager().isFeatureEnabled(DemoFeature.NAME));
			
			assertFalse(DemoFeature2.initWasRun);
			assertFalse(k.getDeploymentManager().isFeatureEnabled(DemoFeature2.NAME));
			
			assertFalse(DemoFeature3.initWasRun);
			assertFalse(k.getDeploymentManager().isFeatureEnabled(DemoFeature3.NAME));
			
		}finally{
			k.shutdown();
		}
	}


	public void testStartKernelUsingPropertiesFile()throws Exception{
		Kernel k=null;
		try
		{
			k=new Kernel("src/test/resources/conf/use.properties");
			k.startSynchronous();

			//defined in properties file
			String test=k.getContainerProperties().getRawProperties().getProperty("test.property");
			assertEquals("foo",test);

			String test2=k.getContainerProperties().getRawProperties().getProperty("test.foo.property");
			assertEquals("spam",test2);
			
			//base URL
			String base = k.getContainerProperties().getBaseUrl();
			//external URL
			String ext = k.getContainerProperties().getContainerURL();
			System.out.println(base);
			System.out.println(ext);
			assertEquals(base,ext+"/services");

		} finally {
			if (k != null)
				k.shutdown();
		}
	}

	public void testLoadClass()throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		k.load(MockClass1.class);
		assertEquals(k, MockClass1.kernel);
		k.load(MockClass2.class);
		assertEquals(k, MockClass2.kernel);
		MockClass3 m3=k.load(MockClass3.class);
		assertNotNull(m3);
	}
	
	public void testStartupTask()throws Exception{
		Properties extra = new Properties();
		extra.setProperty(ContainerProperties.PREFIX+ContainerProperties.ON_STARTUP_KEY, 
				MockStartupTask.class.getName()+" "+MockStartupTask2.class.getName());
		
		int cur = ExampleStartupTask.runCount;
		
		Kernel k=new Kernel("src/test/resources/conf/use.properties", extra);
		assertNotNull(k);
		k.startSynchronous();
		assertTrue(ranStartupTask1);
		assertTrue(ranStartupTask2);
		assertEquals(k, MockStartupTask2.kernel);
		
		//new style
		assertEquals(cur+1, ExampleStartupTask.runCount);
		
		k.shutdown();
	}

	private int lastRun = 0;
	public void testStartupTasksOrder() throws Exception {
		Set<StartupTask> toRun = new HashSet<StartupTask>();
		toRun.add(new AbstractStartupTask() {
			@Override
			public void run() {
				assertEquals(lastRun, 0);
				lastRun=1;
			}
			public String getName() {
				return "a";
			}
			public Set<String> getBefore() {
				return Collections.singleton("b");
			}
		});
		toRun.add(new AbstractStartupTask() {
			@Override
			public void run() {
				assertEquals(lastRun, 1);
				lastRun=2;
			}
			public String getName() {
				return "b";
			}
			public Set<String> getAfter() {
				return Collections.singleton("a");
			}
		});
		toRun.add(new AbstractStartupTask() {
			@Override
			public void run() {
				assertEquals(lastRun, 2);
			}
			public String getName() {
				return "c";
			}
			public Set<String> getAfter() {
				return Collections.singleton("b");
			}
		});
		
		new StartupTasksRunner().runStartupTasks(null, toRun);
	}
	
	private static boolean ranStartupTask1=false;
	private static boolean ranStartupTask2=false;

	public static class MockStartupTask implements Runnable{
		
		public void run(){
			ranStartupTask1=true;
		}			
	}

	public static class MockStartupTask2 implements Runnable{
		
		static Kernel kernel;
		
		public MockStartupTask2(Kernel k){
			kernel=k;
		}
		
		public void run(){
			ranStartupTask2=true;
		}			
	}
	
	public static class MockClass1 implements KernelInjectable{
		static Kernel kernel;
		
		public void setKernel(Kernel k){
			kernel=k;
		}
	}
	
	public static class MockClass2 {
		static Kernel kernel;
		
		public MockClass2(Kernel k){
			kernel=k;
		}
	}
	
	public static class MockClass3 {}
	
}
