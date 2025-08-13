package eu.unicore.services.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.services.rest.testservice.CounterModel;
import eu.unicore.services.rest.testservice.CounterResource;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

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
		
		for(int i=0;i<10;i++){
			HomeApplication.createTestInstance(k);
		}
	}

	@AfterAll
	public static void stop() throws Exception {
		if(k!=null)k.shutdown();
	}

	@Test
	public void testPagedResults()throws Exception {
		assertTrue(k.getHome("counter").getStore().getUniqueIDs().size()>0);
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


	@Test
	public void testAction()throws Exception {
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		String base  = url+"/"+sName+"/foo/"+
				k.getHome("counter").getStore().getUniqueIDs().get(0);
		BaseClient client = new BaseClient(base,k.getClientConfiguration());
		System.out.println(client.getJSON().toString(2));
		assertTrue(client.getLink("action:test")!=null);
		client.pushURL(client.getLink("action:test"));
		JSONObject r = client.asJSON(client.post(new JSONObject()));
		assertEquals("OK", r.get("test"));
		client.popURL();
		client.pushURL(base+"/actions/nosuchaction");
		RESTException re = assertThrows(RESTException.class, ()->{
			client.post(new JSONObject());
		});
		assertEquals(404, re.getStatus());
	}

	@Test
	public void testActionViaHTML()throws Exception {
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		String base  = url+"/"+sName+"/foo/"+
				k.getHome("counter").getStore().getUniqueIDs().get(0);
		BaseClient client = new BaseClient(base,k.getClientConfiguration());
		System.out.println(client.getJSON().toString(2));
		assertTrue(client.getLink("action:test")!=null);
		client.pushURL(client.getLink("action:test"));
		List<NameValuePair>params = new ArrayList<>();
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
		ClassicHttpResponse res = client.post(entity, ContentType.APPLICATION_FORM_URLENCODED);
		String html = EntityUtils.toString(res.getEntity());
		assertTrue(html.contains("<h2>Properties</h2>"));
		XmlObject.Factory.parse(html);
		client.popURL();
		client.pushURL(base+"/actions/nosuchaction");
		RESTException re = assertThrows(RESTException.class, ()->{
			client.post(entity,ContentType.APPLICATION_FORM_URLENCODED);
		});
		assertEquals(404, re.getStatus());
	}

	@Test
	public void testGetAsHTML()throws Exception {
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		String base  = url+"/"+sName+"/foo/"+
				k.getHome("counter").getStore().getUniqueIDs().get(0);
		BaseClient client = new BaseClient(base,k.getClientConfiguration());
		ClassicHttpResponse res = client.get(ContentType.TEXT_HTML);
		String html = EntityUtils.toString(res.getEntity());
		XmlObject.Factory.parse(html);
		// service home endpoint w/ instance list
		base  = url+"/"+sName+"/foo";
		client = new BaseClient(base,k.getClientConfiguration());
		res = client.get(ContentType.TEXT_HTML);
		html = EntityUtils.toString(res.getEntity());
		XmlObject.Factory.parse(html);
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
			k.getHome("counter").createResource(new InitParameters());
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

		@Override
		protected JSONObject doHandleAction(String name, JSONObject o) throws Exception {
			if("test".equals(name)) {
				JSONObject reply = new JSONObject();
				reply.put("test", "OK");
				return reply;
			}
			return super.doHandleAction(name, o);
		}

		@Override
		protected void updateLinks() {
			super.updateLinks();
			links.add(new Link("action:test", getBaseURL() + "/" + getPathComponent() +
					resource.getUniqueID()+"/actions/test", "Test action"));
		}
	}
}
