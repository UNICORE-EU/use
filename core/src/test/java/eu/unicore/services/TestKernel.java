package eu.unicore.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.AbstractStartupTask;
import eu.unicore.services.server.GatewayHandler;
import eu.unicore.services.server.StartupTasksRunner;
import eu.unicore.services.testservice.MockHome;
import eu.unicore.services.utils.deployment.DemoFeature;
import eu.unicore.services.utils.deployment.DemoFeature2;
import eu.unicore.services.utils.deployment.DemoFeature3;

public class TestKernel {

	@AfterEach
	public void tearDown(){
		ranStartupTask1=false;
		ranStartupTask2=false;
	}

	@Test
	public void testHeaders() throws Exception{	
		Kernel k = new Kernel(TestConfigUtil.getInsecureProperties());
		System.out.println(k.getHeader());
		System.out.println(k.getConnectionStatus());
	}

	@Test
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

	@Test
	public void testUSEContainer() throws Exception {
		USEContainer uas=new USEContainer("src/test/resources/conf/use.properties", "TEST");
		uas.startSynchronous();
		uas.getKernel().shutdown();
	}

	@Test
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
			
			//external URL
			String ext = k.getContainerProperties().getContainerURL();
			System.out.println(ext);
			assertFalse(ext.endsWith("/"));
			k.refreshConfig();
		} finally {
			if (k != null)
				k.shutdown();
		}
	}

	@Test
	public void testLoadClass()throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		k.load(MockClass1.class);
		assertEquals(k, MockClass1.kernel);
		k.load(MockClass2.class);
		assertEquals(k, MockClass2.kernel);
		MockClass3 m3=k.load(MockClass3.class);
		assertNotNull(m3);
	}

	@Test
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

	@Test
	public void testStartupTasksOrder() throws Exception {
		Set<StartupTask> toRun = new HashSet<>();
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
		
		StartupTasksRunner.runStartupTasks(null, toRun);
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
