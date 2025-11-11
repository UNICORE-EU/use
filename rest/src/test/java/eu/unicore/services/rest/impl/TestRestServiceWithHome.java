package eu.unicore.services.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.messaging.IMessagingChannel;
import eu.unicore.services.messaging.Message;
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
import eu.unicore.util.ConcurrentAccess;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class TestRestServiceWithHome {

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
		System.out.println(k.getConnectionStatus());
	}

	@AfterAll
	public static void stop() throws Exception {
		if(k!=null)k.shutdown();
	}

	@Test
	public void testRestServiceWithHome()throws Exception {
		HomeApplication.createTestInstance(k);
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		int invocations = 5;
		String resource  = url+"/"+sName+"/my_counter";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, k.getClientConfiguration());
		for(int i=0; i<invocations ; i++){
			try(ClassicHttpResponse response = client.post(null)){
				int status = client.getLastHttpStatus();
				assertEquals(200, status);
				String reply = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
				System.out.println("Service reply: "+reply);
			}
		}
		CounterModel m = (CounterModel) k.getHome("counter").get("my_counter").getModel();
		assertEquals(invocations, m.getCounter());
		client.postQuietly(null);
		assertEquals(200, client.getLastStatus().getStatusCode());
		m = (CounterModel) k.getHome("counter").get("my_counter").getModel();
		assertEquals(invocations+1, m.getCounter());

		resource  = url+"/"+sName+"/my_counter/value";
		client.setURL(resource);
		System.out.println("Accessing "+resource);
		try(ClassicHttpResponse response = client.get(ContentType.WILDCARD)){
			int status = client.getLastHttpStatus();
			assertEquals(200, status);
			String reply = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			System.out.println("Service reply: "+reply);
		}
		// set value
		resource  = url+"/"+sName+"/my_counter/value";
		client.setURL(resource);
		
		String v = "100";
		System.out.println("Putting value: "+v);
		try(ClassicHttpResponse response=client.put(new ByteArrayInputStream(v.getBytes()),ContentType.APPLICATION_JSON)){
			int status=client.getLastHttpStatus();
			assertEquals(200, status);
		}
		resource  = url+"/"+sName+"/my_counter/NO_SUCH_PROPERTY";
		client.setURL(resource);
		try(ClassicHttpResponse response = client.put(new ByteArrayInputStream(v.getBytes()),ContentType.APPLICATION_JSON)){
		}catch(Exception ex) {
			int status=client.getLastHttpStatus();
			assertEquals(404, status);
		}
		
		// get properties
		resource  = url+"/"+sName+"/my_counter";
		client.setURL(resource);
		JSONObject o = client.getJSON();
		System.out.println(o.toString(2));
		try(ClassicHttpResponse response = client.get(ContentType.TEXT_HTML)){
			String html = EntityUtils.toString(response.getEntity());
			assertNotNull(html);
			assertTrue(html.contains("action:foo"));
			assertTrue(html.contains("terminationTime"));
		}
		// check messages to resources are processed properly when using the
		// REST interface
		CounterResource.processedMessages=0;
		IMessagingChannel ch = k.getMessaging().getChannel("my_counter");
		ch.publish(new Message());
		ch.publish(new Message());
		Thread.sleep(50);
		client.getJSON();
		assertEquals(2, CounterResource.processedMessages);
		CounterResource.processedMessages=0;
		client.getJSON();
		assertEquals(0, CounterResource.processedMessages);

		// test exception handling
		client.setURL(resource+"/fail");
		client.setAutomaticErrorChecking(false);
		client.get(ContentType.APPLICATION_JSON);
		// resource must not be locked!
		try{
			Resource r = k.getHome(sName).getStore().getForUpdate("my_counter", 100, TimeUnit.MILLISECONDS);
			k.getHome(sName).getStore().unlock(r);
		}catch(TimeoutException e){
			fail("Resource is locked!");
		}
		client.setURL(resource+"/nosuchmethod");
		client.get(ContentType.APPLICATION_JSON);
		// resource must not be locked!
		try{
			Resource r = k.getHome(sName).getStore().getForUpdate("my_counter", 100, TimeUnit.MILLISECONDS);
			k.getHome(sName).getStore().unlock(r);
		}catch(TimeoutException e){
			fail("Resource is locked!");
		}
		client.setURL(resource);
		client.delete();
		// assert that resource is gone
		assertFalse(k.getHome(sName).getStore().getUniqueIDs().contains("my_counter"));
		
		// create instance dynamically
		client.setURL(url+"/"+sName);
		String loc = client.create(new JSONObject());
		System.out.println("Created new instance "+loc);
		client.setURL(loc);
		client.delete();
		client.setURL(url+"/"+sName);
		// just to check the quiet method
		client.postQuietly(new JSONObject());
	}

	@Test
	public void testSetProperties()throws Exception {
		HomeApplication.createTestInstance(k);
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		BaseClient client=new BaseClient(url,k.getClientConfiguration());
		String resource  = url+"/"+sName+"/my_counter";
		client.setURL(resource);
		client.postQuietly(null);
		// set properties via PUT
		JSONObject setP = new JSONObject();
		JSONArray aclE = new JSONArray();
		aclE.put("modify:DN:CN=Demo");
		aclE.put("modify:DN:CN=Someone Else");
		setP.put("acl", aclE);
		setP.put("terminationTime", "23:59");
		
		setP.put("tags","test,foo,bar");
		setP.put("noSuchProperty", "foo");
		try(ClassicHttpResponse response = client.put(setP)){
			int status=client.getLastHttpStatus();
			assertEquals(200, status);
			String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			System.out.println("Service reply: "+reply);
			JSONObject replyJ = new JSONObject(reply);
			assertEquals("OK",replyJ.getString("acl"));
			assertEquals("OK",replyJ.getString("terminationTime"));
			assertTrue(replyJ.getString("noSuchProperty").contains("not found"));
		}
		JSONObject o = client.getJSON();
		System.out.println(o.toString(2));
		assertEquals("modify:DN:CN=Demo",o.getJSONArray("acl").get(0));
		assertEquals("modify:DN:CN=Someone Else",o.getJSONArray("acl").get(1));

		// provoke error in setProperty
		setP = new JSONObject();
		setP.put("acl", "garble:DN:CN=Demo");
		try(ClassicHttpResponse response = client.put(setP)){
			assertEquals(200, client.getLastHttpStatus());
			String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			JSONObject replyJ = new JSONObject(reply);
			assertTrue(replyJ.getString("acl").contains("Error setting property"));
		}
		// same with ignoring error checking
		client.putQuietly(setP);
		client.delete();
	}

	@Test
	public void testExceptionResponse() throws Exception {
		HomeApplication.createTestInstance(k);
		String url = k.getContainerProperties().getContainerURL()+"/rest";
		BaseClient client=new BaseClient(url,k.getClientConfiguration());
		String resource  = url+"/"+sName+"/my_counter";
		client.setURL(resource);
		System.out.println("Accessing "+resource);
		MockResourceWithHome.fail_on_getproperties = true;
		try{
			client.getJSON();
			fail("Expected exception here");
		}catch(RESTException re) {
			assertEquals(500, re.getStatus());
			assertTrue(re.getErrorMessage().contains("Failure for testing"));
		}
		client.delete();
		MockResourceWithHome.fail_on_getproperties = false;
	}


	public static class HomeApplication extends Application implements USERestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes = new HashSet<>();
			classes.add(MockResourceWithHome.class);
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
			createTestInstance(k,false);
		}
	
		public static String createTestInstance(Kernel k, boolean random) throws Exception {
			Home home = k.getHome("counter");
			String uid = random ? null : "my_counter";
			InitParameters initParams = new InitParameters(uid);
			String id = home.createResource(initParams);
			System.out.println("Created test instance <"+id+">");
			return id;
		}

	}

	public static class MockHome extends DefaultHome {
		@Override
		protected Resource doCreateInstance() throws Exception {
			return new CounterResource();
		}
	}

	@Path("/")
	@USEResource(home="counter")
	public static class MockResourceWithHome extends BaseRESTController {

		@POST
		@Path("/{uniqueID}")
		public String increment(@PathParam("uniqueID") String name){
			int invocations = getModel().getCounter();
			invocations++;
			String s="Value of "+name+" is <"+invocations+">";
			getModel().setCounter(invocations);
			return s;
		}

		@ConcurrentAccess(allow=true)
		@GET
		@Path("/{uniqueID}/value")
		public String getValue(@PathParam("uniqueID") String name){
			int invocations = getModel().getCounter();
			String s="Value of "+name+" is <"+invocations+">";
			getModel().setCounter(invocations);
			return s;
		}

		@PUT
		@Path("/{uniqueID}/{property}")
		@Consumes(MediaType.APPLICATION_JSON)
		public String setProperty(@PathParam("uniqueID") String name, @PathParam("property") String property, 
				InputStream contentStream) throws Exception {
			String content = IOUtils.toString(contentStream, "UTF-8");
			getModel().setCounter(Integer.parseInt(content));
			if(!"value".equals(property)){
				throw new WebApplicationException(new IllegalArgumentException("No such property:"+property),404);
			}
			String s="New value of "+name+" is <"+getModel().getCounter()+">";
			return s;
		}

		@Override
		public CounterModel getModel(){
			return (CounterModel)super.getModel();
		}

		@Override
		protected void updateLinks() {
			super.updateLinks();
			links.add(new Link("action:foo",getBaseURL()+"/"+resource.getUniqueID()+"/actions/foo","Foo"));
		}

		@GET
		@Path("/{uniqueID}/fail")
		public String justFail(){
			throw new WebApplicationException(500);
		}

		@POST
		@Consumes("application/json")
		@Path("/")
		public Response createNew() throws Exception {
			String id = HomeApplication.createTestInstance(k, true);
			return Response.created(new URI(getBaseURL()+"/"+id)).build();
		}

		public static boolean fail_on_getproperties = false;

		@Override
		protected Map<String, Object> getProperties() throws Exception {
			if(fail_on_getproperties)throw new Exception("Failure for testing");
			return super.getProperties();
		}

		@Override
		public boolean usesKernelMessaging() {
			return true;
		}

	}

}
