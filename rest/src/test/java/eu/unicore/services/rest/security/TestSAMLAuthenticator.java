package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.AudienceRestrictionType;
import xmlbeans.org.oasis.saml2.assertion.AuthnStatementType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseType;

public class TestSAMLAuthenticator {

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
		p.setProperty("container.security.accesscontrol", "true");
		p.setProperty("container.security.accesscontrol.pdp",
				"eu.unicore.services.security.pdp.AcceptingPdp");

		p.setProperty("container.security.rest.authentication.order", "SAML SAML2");
		p.setProperty("container.security.rest.authentication.SAML.class",
				UnitySAMLAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.SAML.address",
				"http://localhost:55333/rest/idp/saml");
		p.setProperty("container.security.rest.authentication.SAML.validate","false");

		p.setProperty("container.security.rest.authentication.SAML2.class",
				UnityOAuthAuthenticator.class.getName());
		p.setProperty("container.security.rest.authentication.SAML2.address",
				"http://localhost:55333/rest/idp/saml");
		p.setProperty("container.security.rest.authentication.SAML2.validate","false");
		p.setProperty("container.security.rest.authentication.SAML2.roleAssign",
				"'user'");

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
	}

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.shutdown();
	}

	@Test
	public void test1() throws Exception {
		String resource = url+"/"+sName+"/User";
		UsernamePassword auth = new UsernamePassword("demouser", "test123");
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		System.out.println("Accessing: "+resource);
		JSONObject reply = bc.getJSON();
		System.out.println("Service reply: "+reply.toString(2));
		assertEquals("CN=demouser,OU=saml",
				reply.getJSONObject("client").getString("dn"));
		assertEquals("UNITY-SAML",
				reply.getJSONObject("client").getString("authenticationMethod"));
	}

	@Test
	public void test2() throws Exception {
		String resource = url+"/"+sName+"/User";
		IAuthCallback auth = (msg) -> {
			msg.addHeader("Authorization", "Bearer test123");
		};
		BaseClient bc = new BaseClient(resource, kernel.getClientConfiguration(), auth);
		System.out.println("Accessing: "+resource);
		JSONObject reply = bc.getJSON();
		System.out.println(reply.toString(2));
		assertEquals("CN=demouser,OU=saml",
				reply.getJSONObject("client").getString("dn"));
		assertEquals("UNITY-SAML",
				reply.getJSONObject("client").getString("authenticationMethod"));
		assertEquals("user",
				reply.getJSONObject("client").getJSONObject("role").getString("selected"));
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
		static final AtomicInteger invocationCounter=new AtomicInteger(0);

		@POST
		@Path("/saml")
		@Consumes("*/*")
		@Produces("application/xml")
		public String getRepresentation(String xml) throws Exception {
			invocationCounter.incrementAndGet();
			Document soapenv = (Document)XmlObject.Factory.parse(xml).newDomNode();
			NodeList nl = soapenv.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "AuthnRequest");
			AuthnRequestDocument ard = AuthnRequestDocument.Factory.parse(nl.item(0));

			ResponseDocument rd = ResponseDocument.Factory.newInstance();
			ResponseType rt = rd.addNewResponse();
			rt.setID(ard.getAuthnRequest().getID());
			rt.setIssueInstant(Calendar.getInstance());
			rt.setVersion(SAMLConstants.SAML2_VERSION);
			rt.setDestination(ard.getAuthnRequest().getAssertionConsumerServiceURL());
			rt.addNewStatus().addNewStatusCode().setValue(SAMLConstants.Status.STATUS_OK.toString());

			Calendar notOnOrAfter = Calendar.getInstance();
			notOnOrAfter.setTimeInMillis(System.currentTimeMillis()+100000);
			AssertionType ass1 = rt.addNewAssertion();
			ass1.setID("foo_123");
			ass1.setIssueInstant(Calendar.getInstance());
			ass1.setVersion(SAMLConstants.SAML2_VERSION);
			NameIDType issuer = NameIDType.Factory.newInstance();
			issuer.setStringValue("bar@foo");
			ass1.setIssuer(issuer);
			
			AuthnStatementType at = ass1.addNewAuthnStatement();
			at.setAuthnInstant(Calendar.getInstance());
			NameIDType subj = NameIDType.Factory.newInstance();
			subj.setFormat(SAMLConstants.NFORMAT_DN);
			subj.setStringValue("CN=demouser,OU=saml");
			ass1.addNewSubject().setNameID(subj);
			SubjectConfirmationType sct = ass1.getSubject().addNewSubjectConfirmation();
			sct.setMethod(SAMLConstants.CONFIRMATION_BEARER);
			SubjectConfirmationDataType confData = sct.addNewSubjectConfirmationData();
			confData.setRecipient("http://localhost:55333");
			confData.setNotOnOrAfter(notOnOrAfter);
			confData.setInResponseTo(ard.getAuthnRequest().getID());
			AudienceRestrictionType aud = ass1.addNewConditions().addNewAudienceRestriction();
			aud.addAudience("http://localhost:55333");
			ass1.getConditions().setNotOnOrAfter(notOnOrAfter);

			return idpReply(rd);
		}

		public String idpReply(ResponseDocument rd) throws Exception {
			return "<?xml version = \"1.0\"?>"+
			"<se:Envelope xmlns:se =\"http://schemas.xmlsoap.org/soap/envelope/\">"+
			"<se:Body>"+rd.toString()+"</se:Body></se:Envelope>";
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
