package eu.unicore.services.rest.testservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.sshkey.Password;
import eu.unicore.services.rest.security.sshkey.SSHKey;
import eu.unicore.services.rest.security.sshkey.SSHKeyUC;
import eu.unicore.services.rest.security.sshkey.SSHUtils;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HttpUtils;

public class TestRestSecurity {

	static Kernel kernel;
	static String sName="test";
	static String url;
	
	@BeforeClass
	public static void startServer()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		kernel=new Kernel("src/test/resources/use.properties");
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

	@AfterClass
	public static void stopServer()throws Exception{
		kernel.shutdown();
	}


	@Test
	public void testSecuritySessions()throws Exception {
		
		int invoked=MockResource.invocationCounter.get();
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get=new HttpGet(url+"/"+sName+"/User");
		IAuthCallback pwd = new UsernamePassword("demouser", "test123");
		pwd.addAuthenticationHeaders(get);
		HttpResponse response=client.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals(200, status);
		assertEquals(invoked+1, MockResource.invocationCounter.get());
		JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
		System.out.println("Service reply: "+reply.toString(2));
		assertTrue(checkSessionInfo(response));
		String sessionID = response.getFirstHeader(SecuritySessionUtils.SESSION_ID_HEADER).getValue();
		get=new HttpGet(url+"/"+sName+"/User");
		get.addHeader(SecuritySessionUtils.SESSION_ID_HEADER, sessionID);
		response=client.execute(get);
		status=response.getStatusLine().getStatusCode();
		assertEquals(200, status);
		assertTrue(checkSessionInfo(response));

		// invalid session must give a 432 response
		get=new HttpGet(url+"/"+sName+"/User");
		get.addHeader(SecuritySessionUtils.SESSION_ID_HEADER, "123");
		response=client.execute(get);
		status=response.getStatusLine().getStatusCode();
		assertEquals(432, status);
		assertFalse(checkSessionInfo(response));
	}

	private boolean checkSessionInfo(HttpResponse response){
		try{
			String sessionID = response.getFirstHeader(SecuritySessionUtils.SESSION_ID_HEADER).getValue();
			String lifetime = response.getFirstHeader(SecuritySessionUtils.SESSION_LIFETIME_HEADER).getValue();
			System.out.println("(Re-)using security session: ID = " + sessionID + " lifetime = "+lifetime);
			return true;
		}catch(Exception ex){
			return false;
		}
	}
	
	
	@Test
	public void testJWTDelegationAuth() throws Exception {
		String dn = "CN=Demo User, O=UNICORE, C=EU";
		String issuer = "CN=UNICOREX,O=UNICORE,C=EU";
		
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get=new HttpGet(url+"/"+sName+"/User");
		JWTServerProperties props = new JWTServerProperties(new Properties());
		IAuthCallback pwd = new JWTDelegation(kernel.getContainerSecurityConfiguration(), props, dn);
		pwd.addAuthenticationHeaders(get);
		HttpResponse response=client.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals(200, status);
		JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
		System.out.println("Service reply: "+reply.toString(2));
		Assert.assertEquals(dn, reply.getString("dn"));
		Assert.assertEquals(issuer, reply.getString("td_consignor"));
		Assert.assertTrue(Boolean.parseBoolean(String.valueOf(reply.get("td_status"))));
	}
	

	@Test
	public void testBaseClientWithDelegation() throws Exception {
		String dn = "CN=Demo User, O=UNICORE, C=EU";
		String issuer = "CN=UNICOREX,O=UNICORE,C=EU";
		String resource = url+"/"+sName+"/User";

		JWTServerProperties props = new JWTServerProperties(new Properties());
		IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), props, dn);
		
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		String sessionId = null;
		
		for(int i=1; i<3; i++){
			JSONObject reply = bc.getJSON();
			if(i==1){
				System.out.println("Service reply: "+reply.toString(2));
				sessionId = bc.getSessionIDProvider().getSessionID(resource, bc.getSessionKey());
				assertNotNull(sessionId);
			}
			else{
				assertEquals(sessionId, bc.getSessionIDProvider().getSessionID(resource, 
						bc.getSessionKey()));
			}
			Assert.assertEquals(dn, reply.getString("dn"));
			Assert.assertEquals(issuer, reply.getString("td_consignor"));
			Assert.assertTrue(Boolean.parseBoolean(String.valueOf(reply.get("td_status"))));
		}
		
	}
	
	@Test
	public void testBaseClientWithJWT() throws Exception {
		String resource = url+"/"+sName+"/User";
		IAuthCallback auth = new SSHKey("demouser", new File("src/test/resources/id_ed25519"),
				new Password("test123".toCharArray()));
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
	}

	@Test
	public void testBaseClientWithLegacySSHKey() throws Exception {
		String resource = url+"/"+sName+"/User";
		SSHKeyUC auth = SSHUtils.createAuthData(new File("src/test/resources/id_ed25519"),
				"test123".toCharArray(), String.valueOf(System.currentTimeMillis()));
		auth.username = "demouser";
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
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
		@Produces("application/json")
		public String getRepresentation(@PathParam("uniqueID") String name) throws JSONException {
			invocationCounter.incrementAndGet();
			
			JSONObject res = new JSONObject();
			try{
				res.put("invocations", invocationCounter.get());
				res.put("dn", AuthZAttributeStore.getClient().getDistinguishedName());
				res.put("role", AuthZAttributeStore.getClient().getRole().getName());
				res.put("td_status", AuthZAttributeStore.getTokens().isConsignorTrusted());
				res.put("td_consignor", String.valueOf(AuthZAttributeStore.getTokens().getConsignorName()));
			}catch(Exception ex){
				ex.printStackTrace();
				res.put("error", Log.createFaultMessage("", ex));
			}
			return res.toString();
		}
	}
	
}
