package eu.unicore.services.rest.testservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.server.JettyServer;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.rest.RestService;
import eu.unicore.util.httpclient.HttpUtils;

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
		String sName="test";
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setKernel(kernel);
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setName(sName);
		kernel.getDeploymentManager().deployService(dd);

		int invoked=MockResource.invocationCounter.get();

		JettyServer server=kernel.getServer();
		String url = server.getUrls()[0].toExternalForm()+"/rest";
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get=new HttpGet(url+"/"+sName+"/User");
		get.addHeader("Accept", "application/json");
		HttpResponse response=client.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals(200, status);
		assertEquals(invoked+1, MockResource.invocationCounter.get());
		String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		System.out.println("Service reply: "+reply);
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
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());

		HttpGet get=new HttpGet(url+"/rest/test2/foo");
		HttpResponse response=client.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals(200, status);
		String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		System.out.println("Service 'foo' reply: "+reply);

		get=new HttpGet(url+"/rest/test2/bar");
		response=client.execute(get);
		status=response.getStatusLine().getStatusCode();
		assertEquals(200, status);
		reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		System.out.println("Service 'bar' reply: "+reply);

	}

	@Before
	public void startServer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		kernel=new Kernel(p);
		kernel.start();
	}

	@After
	public void stopServer()throws Exception{
		kernel.shutdown();
	}


	public static class MyApplication extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<Class<?>>();
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
