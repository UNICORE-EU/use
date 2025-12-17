package eu.unicore.services.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.util.X509CertUtils;

import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.restclient.sshkey.PasswordSupplierImpl;
import eu.unicore.services.restclient.sshkey.SSHKey;
import eu.unicore.services.restclient.sshkey.SSHKeyUC;
import eu.unicore.services.restclient.sshkey.SSHUtils;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.util.httpclient.HttpUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;

public class TestRestSecurity {

	static Kernel kernel;

	static String sName = "test";

	static String url;

	@BeforeAll
	public static void startServer()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		kernel = new Kernel("src/test/resources/use.properties");
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
	}

	@Test
	public void testSecuritySessions()throws Exception {
		int invoked=MockResource.invocationCounter.get();
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get=new HttpGet(url+"/"+sName+"/User");
		IAuthCallback pwd = new UsernamePassword("demouser", "test123");
		pwd.addAuthenticationHeaders(get);
		try(ClassicHttpResponse response=client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(200, response.getCode());
			assertEquals(invoked+1, MockResource.invocationCounter.get());
			JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			System.out.println("Service reply: "+reply.toString(2));
			assertTrue(checkSessionInfo(response));
			String sessionID = response.getFirstHeader(SecuritySessionUtils.SESSION_ID_HEADER).getValue();
			get=new HttpGet(url+"/"+sName+"/User");
			get.addHeader(SecuritySessionUtils.SESSION_ID_HEADER, sessionID);
		}
		try(ClassicHttpResponse response=client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(200, response.getCode());
			assertTrue(checkSessionInfo(response));
		}
		// invalid session must give a 432 response
		get=new HttpGet(url+"/"+sName+"/User");
		get.addHeader(SecuritySessionUtils.SESSION_ID_HEADER, "123");
		try(ClassicHttpResponse response=client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(432, response.getCode());
			assertFalse(checkSessionInfo(response));
		}
	}

	private boolean checkSessionInfo(ClassicHttpResponse response){
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
		try(ClassicHttpResponse response=client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(200, response.getCode());
			JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			System.out.println("Service reply: "+reply.toString(2));
			assertEquals(dn, reply.getString("dn"));
			assertEquals(issuer, reply.getString("td_consignor"));
			assertTrue(Boolean.parseBoolean(String.valueOf(reply.get("td_status"))));
			assertEquals("ETD", reply.getString("auth_method"));
		}
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
			assertEquals(dn, reply.getString("dn"));
			assertEquals(issuer, reply.getString("td_consignor"));
			assertEquals("ETD", reply.getString("auth_method"));
			assertTrue(Boolean.parseBoolean(String.valueOf(reply.get("td_status"))));
		}
	}
	
	@Test
	public void testBaseClientWithJWT() throws Exception {
		String resource = url+"/"+sName+"/User";
		IAuthCallback auth = new SSHKey("demouser", new File("src/test/resources/id_ed25519"),
				new PasswordSupplierImpl("test123".toCharArray()));
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
		assertEquals("SSHKEY", reply.getString("auth_method"));
	}

	@Test
	public void testIssueToken() throws Exception {
		long now = System.currentTimeMillis();
		long tokenValidity = new JWTServerProperties(kernel.getContainerProperties().getRawProperties())
				.getTokenValidity();
		long exp = (long)(now/1000) + tokenValidity;
		String resource = url+"/"+sName+"/token";
		IAuthCallback auth = new SSHKey("demouser", new File("src/test/resources/id_ed25519"),
				new PasswordSupplierImpl("test123".toCharArray()));
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		try(ClassicHttpResponse response=bc.get(ContentType.TEXT_PLAIN)){
			assertEquals(200, response.getCode());
			String token = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			JSONObject t = JWTUtils.getPayload(token);
			System.out.println(t.toString(2));
			assertEquals("CN=Demo User, O=UNICORE, C=EU", t.get("sub"));
			assertTrue(t.getLong("exp")>=exp);
		}
	}

	@Test
	public void testIssueTokenLifetimeError() throws Exception {
		long notAfter = kernel.getContainerSecurityConfiguration().getCredential().
				getCertificate().getNotAfter().getTime();
		long remaining = notAfter - System.currentTimeMillis();
		long request = (long)(1.1 * remaining);
		String resource = url+"/"+sName+"/token?lifetime="+request;
		IAuthCallback auth = new SSHKey("demouser", new File("src/test/resources/id_ed25519"),
				new PasswordSupplierImpl("test123".toCharArray()));
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		RESTException re = assertThrows(RESTException.class, ()-> bc.get(ContentType.TEXT_PLAIN));
		assertEquals(HttpStatus.SC_BAD_REQUEST, re.getStatus());
		System.out.println(re.getErrorMessage());
		assertTrue(re.getErrorMessage().contains("token lifetime"));
	}

	@Test
	public void testGetCert() throws Exception {
		String resource = url+"/"+sName+"/certificate";
		IAuthCallback auth = new SSHKey("demouser", new File("src/test/resources/id_ed25519"),
				new PasswordSupplierImpl("test123".toCharArray()));
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		try(ClassicHttpResponse response=bc.get(ContentType.TEXT_PLAIN)){
			assertEquals(200, response.getCode());
			String pem = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			X509Certificate cert = X509CertUtils.parse(pem);
			assertEquals("CN=UNICOREX,O=UNICORE,C=EU", 
					cert.getSubjectX500Principal().getName());
		}
	}
	
	@Test
	public void testQueryFields()throws Exception {
		HttpClient client = HttpUtils.createClient(url, kernel.getClientConfiguration());
		// only show certain fields
		HttpGet get = new HttpGet(url+"/"+sName+"/User?fields=role,dn");
		IAuthCallback pwd = new UsernamePassword("demouser", "test123");
		pwd.addAuthenticationHeaders(get);
		try(ClassicHttpResponse response = client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(200, response.getCode());
			JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			System.out.println(reply.toString(2));
			Set<String>keys = reply.keySet();
			assertEquals(2, keys.size());
			assertFalse(keys.contains("auth_method"));
			assertTrue(keys.contains("dn"));
			assertTrue(keys.contains("role"));
		}
		// exclude fields
		get = new HttpGet(url+"/"+sName+"/User?fields=!role,!dn");
		pwd.addAuthenticationHeaders(get);
		try(ClassicHttpResponse response = client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(200, response.getCode());
			JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			System.out.println(reply.toString(2));
			Set<String>keys = reply.keySet();
			assertTrue(keys.size()>0);
			assertFalse(keys.contains("dn"));
			assertFalse(keys.contains("role"));
		}
		
	}

	@Test
	public void testUserPreferences()throws Exception {
		HttpClient client = HttpUtils.createClient(url, kernel.getClientConfiguration());
		// only show certain fields
		HttpGet get = new HttpGet(url+"/"+sName+"/User?fields=preferences");
		IAuthCallback pwd = new UsernamePassword("preftest", "test123");
		pwd.addAuthenticationHeaders(get);
		String prefs = "group:spam,xlogin:nobody2,supplementaryGroups:bar,role:admin";
		get.addHeader(AuthNHandler.USER_PREFERENCES_HEADER, prefs);
		try(ClassicHttpResponse response = client.executeOpen(null, get, HttpClientContext.create())){
			assertEquals(200, response.getCode());
			JSONObject reply = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			Set<String>keys = reply.keySet();
			assertEquals(1, keys.size());
			assertTrue(keys.contains("preferences"));
			JSONObject active = reply.getJSONObject("preferences");
			assertEquals("admin", active.getJSONArray("role").get(0));
			assertEquals("nobody2", active.getJSONArray("xlogin").get(0));
			assertEquals("spam", active.getJSONArray("group").get(0));
			assertEquals("bar", active.getJSONArray("supplementaryGroups").get(0));
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
			if(wantProperty("preferences")){
				properties.put("preferences", AuthZAttributeStore.getTokens().getUserPreferences());
			}
			return properties;
		}
	}
}
