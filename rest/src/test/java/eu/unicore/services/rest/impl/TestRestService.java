package eu.unicore.services.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Application;

public class TestRestService {

	Kernel kernel;

	@Test
	public void testDeployRestService()throws Exception {
		String sName="rest";
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setKernel(kernel);
		dd.setName(sName);
		kernel.getDeploymentManager().deployService(dd);
		RestService mock=(RestService)kernel.getService(sName);
		assertNotNull(mock);
		assertEquals(sName,mock.getName());
		assertTrue(mock.isStarted());
	}

	@Test
	public void testInvokeRestService()throws Exception {
		String sName = "test";
		kernel.getContainerProperties().setProperty("messageLogging.test", "true");
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setKernel(kernel);
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setName(sName);
		kernel.getDeploymentManager().deployService(dd);
		int invoked = MockResource.invocationCounter.get();
		JettyServer server = kernel.getServer();
		String url = server.getUrls()[0].toExternalForm()+"/rest/"+sName+"/User";
		BaseClient client = new BaseClient(url, kernel.getClientConfiguration());
		try(ClassicHttpResponse response = client.get(null)){
			assertEquals(200, response.getCode());
			assertEquals(invoked+1, MockResource.invocationCounter.get());
			String reply = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			System.out.println("Service reply: "+reply);
		}
		kernel.getContainerProperties().setProperty("messageLogging.test", "false");
	}

	@Test
	public void testDeployTwoServices()throws Exception {
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setKernel(kernel);
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setName("test1");
		kernel.getDeploymentManager().deployService(dd);

		DeploymentDescriptorImpl dd2 = new DeploymentDescriptorImpl();
		dd2.setKernel(kernel);
		dd2.setType(RestService.TYPE);
		dd2.setImplementation(MyOtherApplication.class);
		dd2.setName("test2");
		kernel.getDeploymentManager().deployService(dd2);

		JettyServer server=kernel.getServer();
		String url = server.getUrls()[0].toExternalForm();
		BaseClient client = new BaseClient(url+"/rest/test2/foo", kernel.getClientConfiguration());
		try(ClassicHttpResponse response = client.get(null)){
			assertEquals(200, response.getCode());
			String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			System.out.println("Service 'foo' reply: "+reply);
		}
		client.setURL(url+"/rest/test2/bar");
		try(ClassicHttpResponse response = client.get(null)){
			assertEquals(200, response.getCode());
			String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			System.out.println("Service 'bar' reply: "+reply);
		}
	}

	@BeforeEach
	public void startServer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		kernel=new Kernel(p);
		kernel.start();
	}

	@AfterEach
	public void stopServer()throws Exception{
		kernel.shutdown();
	}


	public static class MyApplication extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(MockResource.class);
			return classes;
		}
	}


	@Path("/")
	public static class MockResource {

		static final AtomicInteger invocationCounter=new AtomicInteger(0);

		@GET
		@Path("/{uniqueID}")
		public String getRepresentation(@PathParam("uniqueID") String name){
			invocationCounter.incrementAndGet();
			return "Hello "+name+"!";
		}

	}


	public static class MyOtherApplication extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<Class<?>>();
			classes.add(MockOtherResource.class);
			classes.add(MockThirdResource.class);
			return classes;
		}
	}

	@Path("/bar")
	public static class MockOtherResource {
		static final AtomicInteger invocationCounter=new AtomicInteger(0);

		@GET
		public String getTime(){
			invocationCounter.incrementAndGet();
			return new Date().toString();
		}
	}

	@Path("/foo")
	public static class MockThirdResource {
		static final AtomicInteger invocationCounter=new AtomicInteger(0);
		
		@GET
		public String getTime(){
			invocationCounter.incrementAndGet();
			return new Date().toString();
		}	
	}
	
}
