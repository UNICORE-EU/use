package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;

public class TestOAuthAuthenticator {

	static Kernel kernel;
	static String sName="test";
	static String url;

	@BeforeAll
	public static void startServer()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");

		p.setProperty("container.security.rest.authentication.order", "OAUTH OAUTH2");
		p.setProperty("container.security.rest.authentication.OAUTH.class",
				OAuthAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.OAUTH.address",
				"http://localhost:55333/rest/idp/oauth");
		p.setProperty("container.security.rest.authentication.OAUTH.identityAssign",
				"@src/test/resources/identityassign.oauth");
		p.setProperty("container.security.rest.authentication.OAUTH.roleAssign",
				"\"user\"");
		p.setProperty("container.security.rest.authentication.OAUTH.uidAssign",
				"preferredUsername");
		p.setProperty("container.security.rest.authentication.OAUTH.groupsAssign",
				"\"test123\"");

		p.setProperty("container.security.rest.authentication.OAUTH2.class",
				OAuthAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.OAUTH2.address",
				"http://localhost:55333/rest/idp/validate");
		p.setProperty("container.security.rest.authentication.OAUTH2.identityAssign",
				"@src/test/resources/identityassign.oauth");
		p.setProperty("container.security.rest.authentication.OAUTH2.roleAssign",
				"\"user\"");
		p.setProperty("container.security.rest.authentication.OAUTH2.uidAssign",
				"preferredUsername");
		p.setProperty("container.security.rest.authentication.OAUTH2.groupsAssign",
				"\"test123\"");
		p.setProperty("container.security.rest.authentication.OAUTH2.clientID",
				"foo");
		p.setProperty("container.security.rest.authentication.OAUTH2.clientSecret",
				"foosecret");
		p.setProperty("container.security.rest.authentication.OAUTH2.validate",
				"true");

		kernel=new Kernel(p);
		kernel.start();			

		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyIDPApplication.class);
		dd.setName("idp");
		dd.setKernel(kernel);
		kernel.getDeploymentManager().deployService(dd);

		dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setName(sName);
		dd.setKernel(kernel);
		kernel.getDeploymentManager().deployService(dd);

		JettyServer server=kernel.getServer();
		url = server.getUrls()[0].toExternalForm()+"/rest";
		
		System.out.println(kernel.getConnectionStatus());
	}

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.shutdown();
	}

	@Test
	public void test1() throws Exception {
		String resource = url+"/"+sName+"/User";
		IAuthCallback auth = (msg) -> {
			msg.addHeader("Authorization", "Bearer test123");
		};
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
		assertEquals("OAUTH", reply.getJSONObject("client").
				getString("authenticationMethod"));
		assertEquals("UID=demouser@foo.com", reply.getJSONObject("client").
				getString("dn"));
		assertEquals("test123", reply.getJSONObject("client").
				getJSONObject("xlogin").getString("group"));
	}
	
	@Test
	public void test2() throws Exception {
		String resource = url+"/"+sName+"/User";
		IAuthCallback auth = (msg) -> {
			msg.addHeader("Authorization", "Bearer sometoken");
		};
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
		assertEquals("OAUTH", reply.getJSONObject("client").
				getString("authenticationMethod"));
		assertEquals("UID=demouser@foo.com", reply.getJSONObject("client").
				getString("dn"));
		assertEquals("test123", reply.getJSONObject("client").
				getJSONObject("xlogin").getString("group"));
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
	public static class MockResource extends ApplicationBaseResource {

		static final AtomicInteger invocationCounter=new AtomicInteger(0);

		@GET
		@Path("/{uniqueID}")
		@Produces("application/json")
		public String getRepresentation(@PathParam("uniqueID") String name, 
					@QueryParam("fields") String fields) throws Exception {
			invocationCounter.incrementAndGet();
			parsePropertySpec(fields);
			return getJSON().toString();
		}

		@Override
		protected Map<String,Object>getProperties() throws Exception {
			Map<String,Object> properties = super.getProperties();
			properties.put("invocations", invocationCounter.get());
			properties.put("td_status", AuthZAttributeStore.getTokens().isConsignorTrusted());
			properties.put("td_consignor", String.valueOf(AuthZAttributeStore.getTokens().getConsignorName()));
			return properties;
		}
	}

	@PermitAll
	public static class MyIDPApplication extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(MockIDP.class);
			return classes;
		}
	}

	@Path("/")
	public static class MockIDP extends ApplicationBaseResource {

		@GET
		@Path("/oauth")
		@Produces("application/json")
		public String userinfo(String json, @HeaderParam("Authorization") String auth) throws Exception {
			if(!"Bearer test123".equals(auth))throw new WebApplicationException(403);
			return getInfo().toString();
		}

		@POST
		@Path("/validate")
		@Produces("application/json")
		public String validate(@FormParam("client_id")String clientID,
				@FormParam("client_secret")String clientSecret,
				@FormParam("token")String token) throws Exception {
			JSONObject j = new JSONObject();
			if("sometoken".equals(token)) {
				j = getInfo();
				j.put("active", true);
			}
			else {
				j.put("active", false);
			}
			return j.toString();
		}

		private JSONObject getInfo() {
			JSONObject j = new JSONObject();
			j.put("email", "demouser@foo.com");
			j.put("preferredUsername", "nobody");
			return j;
		}
	}
}
