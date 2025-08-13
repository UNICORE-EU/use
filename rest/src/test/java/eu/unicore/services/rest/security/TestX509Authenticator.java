package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;

public class TestX509Authenticator {

	static Kernel kernel;
	static String sName="test";
	static String url;

	@BeforeAll
	public static void startServer()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		Properties p = new Properties();
		p.load(new FileInputStream(new File("src/test/resources/use.properties")));
		p.setProperty("container.security.rest.authentication.order", "X509");
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
		assertEquals("X509", reply.getJSONObject("client").
				getString("authenticationMethod"));
		assertEquals("CN=UNICOREX,O=UNICORE,C=EU", reply.getJSONObject("client").
				getString("dn"));
	}

	@Test
	public void testGWHeaderCheck() throws Exception {
		kernel.getContainerSecurityConfiguration().setGatewayCertificate(
				kernel.getContainerSecurityConfiguration().getCredential().getCertificate());
		X509Authenticator x = new X509Authenticator();
		x.setKernel(kernel);
		SecurityTokens t = new SecurityTokens();
		String consignerInfo = getConsignorInfo(kernel.getContainerSecurityConfiguration());
		assertTrue(x.checkConsignerInfo(consignerInfo, t));
		assertEquals("CN=UNICOREX,O=UNICORE,C=EU", t.getUserName());
		assertTrue(t.isConsignorTrusted());
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

	/**
	 * code from GW ConsignerProducer
	 */
	private String getConsignorInfo(ContainerSecurityProperties props) throws Exception {
		X509Certificate cert = props.getCredential().getCertificate();
		String dn = cert.getSubjectX500Principal().getName();
		String sig = sign(dn, props.getCredential().getKey());
		return "DN=\""+dn+"\";DSIG="+sig;
	}

	private String sign(String toSign, PrivateKey myKey) throws Exception {
		byte[] hashedToken = hash(toSign.getBytes());
		String alg = "RSA".equalsIgnoreCase(myKey.getAlgorithm())?  
				"SHA1withRSA" : "SHA1withDSA";
		Signature signature = Signature.getInstance(alg);
		signature.initSign(myKey);
		signature.update(hashedToken);
		return new String(Base64.encodeBase64(signature.sign())); 
	}

	private byte[] hash(byte[]data) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(data);
		return md.digest();
	}

}
