package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
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
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;

public class TestOAuthAuthenticator {

	static MockIDPServer server;
	static Kernel kernel;
	static String sName="test";
	static String url;

	@BeforeAll
	public static void startServer()throws Exception{
		server = new MockIDPServer();
		server.start();
		FileUtils.deleteQuietly(new File("target/data"));
		Properties p = new Properties();
		p.load(new FileInputStream(new File("src/test/resources/use.properties")));
		p.setProperty("container.security.rest.authentication.order", "OAUTH");
		p.setProperty("container.security.rest.authentication.OAUTH.class",
				OAuthAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.OAUTH.address",
				server.getURI());
		p.setProperty("container.security.rest.authentication.OAUTH.identityAssign",
				"@src/test/resources/identityassign.oauth");
		p.setProperty("container.security.rest.authentication.OAUTH.roleAssign",
				"\"user\"");
		p.setProperty("container.security.rest.authentication.OAUTH.uidAssign",
				"preferredUsername");
		p.setProperty("container.security.rest.authentication.OAUTH.groupsAssign",
				"\"test123\"");

		kernel=new Kernel(p);
		kernel.start();			
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setName(sName);
		dd.setKernel(kernel);
		kernel.getDeploymentManager().deployService(dd);
		JettyServer server=kernel.getServer();
		url = server.getUrls()[0].toExternalForm()+"/rest";
	}

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.shutdown();
		server.stop();
	}

	@Test
	public void test1() throws Exception {
		String resource = url+"/"+sName+"/User";
		IAuthCallback auth = (msg) -> {
			msg.addHeader("Authorization", "Bearer test123");
		};
		server.setAnswer("{'email': 'demouser@foo.com',"
				+ "'preferredUsername': 'nobody'}");
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
		assertEquals("UID=demouser@foo.com", reply.getString("dn"));
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
			properties.put("dn", AuthZAttributeStore.getClient().getDistinguishedName());
			if(wantProperty("role")){
				properties.put("role", AuthZAttributeStore.getClient().getRole().getName());
			}
			properties.put("td_status", AuthZAttributeStore.getTokens().isConsignorTrusted());
			properties.put("td_consignor", String.valueOf(AuthZAttributeStore.getTokens().getConsignorName()));
			properties.put("auth_method", String.valueOf(AuthZAttributeStore.getTokens().getContext().
					get(AuthNHandler.USER_AUTHN_METHOD)));
			return properties;
		}
	}
}
