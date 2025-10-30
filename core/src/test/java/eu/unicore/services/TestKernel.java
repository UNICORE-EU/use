package eu.unicore.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.GatewayHandler;
import eu.unicore.services.testservice.MockHome;
import eu.unicore.services.utils.deployment.DemoFeature;
import eu.unicore.services.utils.deployment.DemoFeature2;
import eu.unicore.services.utils.deployment.DemoFeature3;

public class TestKernel {

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
			String status = gw.getStatusDescription();
			System.out.println(status);
			assertNotNull(status);
			assertTrue(status.contains("N/A"));
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
		try{
			uas.startSynchronous();
		}
		finally{
			uas.getKernel().shutdown();
		}
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
