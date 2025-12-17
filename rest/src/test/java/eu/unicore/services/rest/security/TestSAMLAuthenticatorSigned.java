package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import eu.unicore.services.restclient.RESTException;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;

public class TestSAMLAuthenticatorSigned {

	static Kernel kernel;
	static String sName="test";
	static String url;
	static MockSAMLServer samlServer;

	@BeforeAll
	public static void startServer()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		samlServer = new MockSAMLServer(true);

		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("container.security.sslEnabled", "false");
		p.setProperty("persistence.directory", "target/data");
		p.setProperty("container.security.rest.authentication.order", "SAML SAML2");
		p.setProperty("container.security.rest.authentication.SAML.class",
				UnitySAMLAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.SAML.address",
				"http://localhost:55444/rest/idp/saml");
		p.setProperty("container.security.rest.authentication.SAML.validate","true");
		p.setProperty("container.security.rest.authentication.SAML.roleAssign",
				"'user'");
		p.setProperty("container.security.rest.authentication.SAML.uidAssign",
				"preferredUsername[0]");

		p.setProperty("container.security.rest.authentication.SAML2.class",
				UnityOAuthAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.SAML2.address",
				"http://localhost:55444/rest/idp/saml");
		p.setProperty("container.security.rest.authentication.SAML2.validate","false");
		p.setProperty("container.security.rest.authentication.SAML2.roleAssign",
				"'user'");
		p.setProperty("container.security.rest.authentication.SAML2.uidAssign",
				"preferredUsername[0]");
		p.setProperty("container.client.securitySessions", "false");
		p.setProperty("container.security.credential.format", "jks");
		p.setProperty("container.security.credential.path", "src/test/resources/keystore.jks");
		p.setProperty("container.security.credential.password", "the!njs");
		p.setProperty("container.security.truststore.type", "directory");
		p.setProperty("container.security.truststore.directoryLocations.1", "src/test/resources/cacert.pem");
		p.setProperty("container.security.trustedAssertionIssuers.type", "directory");
		p.setProperty("container.security.trustedAssertionIssuers.directoryLocations.1",
				"src/test/resources/pubkey.pem");
		kernel = new Kernel(p);
		kernel.start();			
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
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
		samlServer.stopServer();
		kernel.shutdown();
	}

	@Test
	public void testValidate() throws Exception {
		invalidateCache();
		MockSAMLServer.sign = true;
		String resource = url+"/"+sName+"/User";
		UsernamePassword auth = new UsernamePassword("demouser", "test123");
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		System.out.println("Accessing: "+resource);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
	}

	@Test
	public void testValidateFail() throws Exception {
		invalidateCache();
		MockSAMLServer.sign = false;
		String resource = url+"/"+sName+"/User";
		UsernamePassword auth = new UsernamePassword("demouser", "test123");
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		System.out.println("Accessing: "+resource);
		RESTException re = assertThrows(RESTException.class, ()->bc.getJSON());
		assertEquals(403, re.getStatus());
	}

	private void invalidateCache() {
		AuthenticatorChain auth = (AuthenticatorChain)kernel.getAttribute(IAuthenticator.class);
		for(IAuthenticator i: auth.getChain()) {
			if(i instanceof BaseRemoteAuthenticator<?>) {
				((BaseRemoteAuthenticator<?>)i).invalidateCache();
			}
		}
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
		public String getBase() throws Exception {
			JSONObject j = new JSONObject();
			renderJSONProperties(j);
			return j.toString();
		}

		@Override
		protected Map<String,Object>getProperties() throws Exception {
			Map<String,Object> properties = super.getProperties();
			properties.put("invocations", invocationCounter.get());
			return properties;
		}
	}
}
