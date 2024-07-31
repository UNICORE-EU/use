package eu.unicore.services.rest.testservice;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;

public class TestServicesBase {

	static Kernel k;
	static String sName="counter";

	@BeforeAll
	public static void setup() throws Exception {
		FileUtils.deleteQuietly(new File("target/data"));
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("persistence.directory", "target/data");
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		k=new Kernel(p);
		k.start();
		
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(HomeApplication.class);
		dd.setName(sName);
		dd.setKernel(k);
		k.getDeploymentManager().deployService(dd);
		
	}

	@AfterAll
	public static void stop() throws Exception {
		if(k!=null)k.shutdown();
	}

	@Test
	public void testPagedResults()throws Exception {
		System.out.println(k.getHome("counter").getStore().getUniqueIDs());
		for(int i=0;i<10;i++){
			HomeApplication.createTestInstance(k);
		}
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		
		String base  = url+"/"+sName+"/foo";

		// 1. get all with default params
		String resource  = base;
		BaseClient client=new BaseClient(resource,k.getClientConfiguration());
		System.out.println("Accessing "+resource);
		JSONObject o = client.getJSON();
		System.out.println(o.toString(2));
		assertEquals(10, o.getJSONArray("foo").length());
		assertNotNull(o.optJSONObject("_links"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("self"));

		// 2. get 5 elements starting at 0
		resource  = base+"?offset=0&num=5";
		client.setURL(resource);
		System.out.println("Accessing "+resource);
		o = client.getJSON();
		System.out.println(o.toString(2));
		assertEquals(5, o.getJSONArray("foo").length());
		assertNotNull(o.optJSONObject("_links"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("self"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("next"));


		// 3. get 5 elements starting at 5
		resource  = base+"?offset=5&num=5";
		client.setURL(resource);
		System.out.println("Accessing "+resource);
		o = client.getJSON();
		System.out.println(o.toString(2));
		assertEquals(5, o.getJSONArray("foo").length());
		assertNotNull(o.optJSONObject("_links"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("self"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("previous"));

		// 4. get 2 elements starting at 2
		resource  = base+"?offset=2&num=2";
		client.setURL(resource);
		System.out.println("Accessing "+resource);
		o = client.getJSON();
		System.out.println(o.toString(2));
		assertEquals(2, o.getJSONArray("foo").length());
		assertNotNull(o.optJSONObject("_links"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("self"));
		assertNotNull(o.getJSONObject("_links").optJSONObject("previous"));
	}



	public static class HomeApplication extends Application implements USERestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(MockResource.class);
			return classes;
		}

		@Override
		public void initialize(Kernel kernel)throws Exception {
			Home home = new MockHome();
			home.setKernel(kernel);
			home.start("counter");
			kernel.putHome(home);
		}

		public static void createTestInstance(Kernel k) throws Exception {
			Home home = k.getHome("counter");
			String id = home.createResource(new InitParameters());
			System.out.println("Created test instance <"+id+">");
		}

	}

	public static class MockHome extends DefaultHome {
		@Override
		protected Resource doCreateInstance() throws Exception {
			return new CounterResource();
		}
	}

	@Path("/foo")
	@USEResource(home="counter")
	public static class MockResource extends ServicesBase {

		@Override
		public CounterModel getModel(){
			return (CounterModel)super.getModel();
		}

	}



}
