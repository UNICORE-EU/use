package eu.unicore.services.aip.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.ExternalSystemConnector.Status;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.AttributeStatementType;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseType;

public class TestSAMLAttributeSource {

	static Kernel kernel;
	static String url;

	@BeforeAll
	public static void startServer()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");

		kernel=new Kernel(p);
		kernel.start();			
		
		// mock attribute query service
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(MockSAMLServerApp.class);
		dd.setName("saml");
		dd.setKernel(kernel);
		kernel.getDeploymentManager().deployService(dd);

		JettyServer server=kernel.getServer();
		url = server.getUrls()[0].toExternalForm()+"/rest";
	}

	@AfterAll
	public static void stopServer()throws Exception{
		kernel.shutdown();
	}

	private SAMLAttributeSource getSAMLAttributeSource() {
		SAMLAttributeSource sas = new SAMLAttributeSource();
		sas.setConfigurationFile("src/test/resources/vo-pull.config");
		sas.configure("SAML", kernel);
		System.out.println(sas.getExternalSystemName()+": "+sas.getConnectionStatus());
		System.out.println(sas.specialAttrsHandler.toString());
		return sas;
	}

	@Test
	public void test1() throws Exception {
		SAMLAttributeSource sas = getSAMLAttributeSource();
		assertNotNull(sas);
		System.out.println(sas+": "+sas.getConnectionStatusMessage());
		assertEquals(Status.OK, sas.getConnectionStatus());
		SecurityTokens tokens = new SecurityTokens();
		tokens.setUserName("CN=Stanisław Lem, C=PL");
		tokens.setConsignorTrusted(true);
		SubjectAttributesHolder holder = sas.getAttributes(tokens, null);
		// have uid, role, groups as attributes
		assertEquals(3, holder.getIncarnationAttributes().size());
		assertEquals(3, holder.getDefaultIncarnationAttributes().size());
		// two valid uids with 'demouser' as default
		assertEquals(2, holder.getValidIncarnationAttributes().get("xlogin").length);
		assertEquals("demouser", holder.getIncarnationAttributes().get("xlogin")[0]);
		assertEquals("demouser", holder.getDefaultIncarnationAttributes().get("xlogin")[0]);

		// one valid role with 'user' as default
		assertEquals(1, holder.getValidIncarnationAttributes().get("role").length);
		assertEquals("user", holder.getIncarnationAttributes().get("role")[0]);
		assertEquals("user", holder.getDefaultIncarnationAttributes().get("role")[0]);

		// two valid groups
		assertEquals(2, holder.getValidIncarnationAttributes().get("group").length);
		assertEquals("users", holder.getValidIncarnationAttributes().get("group")[0]);

		// one extra thingy
		assertEquals(1, holder.getXacmlAttributes().size());
	}


	@PermitAll
	public static class MockSAMLServerApp extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(MockSAMLEndpoint.class);
			return classes;
		}
	}

	@Path("/")
	public static class MockSAMLEndpoint extends ApplicationBaseResource {
		@POST
		@Path("/attributeQuery")
		@Consumes("*/*")
		@Produces("application/xml; charset=UTF-8")
		public String getAttributes(String xml) throws Exception {
			try {
				return doGetAttributes(xml);
			}catch(Exception ex) {
				ex.printStackTrace();
				throw new WebApplicationException(ex.getMessage(), ex, 500);
			}
		}

		private String doGetAttributes(String xml) throws Exception {
			Document soapenv = (Document)XmlObject.Factory.parse(xml).newDomNode();
			NodeList nl = soapenv.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "AttributeQuery");
			AttributeQueryDocument aqd = AttributeQueryDocument.Factory.parse(nl.item(0));
			System.out.println(aqd.toString());
			SubjectType subj = aqd.getAttributeQuery().getSubject();
			ResponseDocument rd = ResponseDocument.Factory.newInstance();
			ResponseType rt = rd.addNewResponse();
			rt.setID(aqd.getAttributeQuery().getID());
			rt.setIssueInstant(Calendar.getInstance());
			rt.setVersion(SAMLConstants.SAML2_VERSION);
			rt.addNewStatus().addNewStatusCode().setValue(SAMLConstants.Status.STATUS_OK.toString());
			NameIDType issuer = NameIDType.Factory.newInstance();
			issuer.setStringValue("bar@foo");
			rt.setIssuer(issuer);

			AssertionType ass1 = rt.addNewAssertion();
			ass1.setID("foo_"+System.currentTimeMillis());
			ass1.setIssueInstant(Calendar.getInstance());
			ass1.setVersion(SAMLConstants.SAML2_VERSION);
			ass1.setIssuer(issuer);
			ass1.setSubject(subj);
			String clientName = subj.getNameID().getStringValue();
			addAttributeStatement(ass1, clientName);
			String r = createSoapReply(rd);
			return r;
		}

		private void addAttributeStatement(AssertionType assertion, String clientName) throws Exception {
			AttributeStatementType ast = assertion.addNewAttributeStatement();
			if(!clientName.equals(Client.ANONYMOUS_CLIENT_DN)){
				// add standard attributes which will be mapped to UNICORE attributes
				// xlogins
				AttributeType at = ast.addNewAttribute();
				at.setName("urn:unicore:attrType:xlogin");
				XmlString p = XmlString.Factory.newInstance();
				p.setStringValue("demouser");
				at.addNewAttributeValue().set(p);

				p = XmlString.Factory.newInstance();
				p.setStringValue("hpc1");
				at.addNewAttributeValue().set(p);

				// role
				at = ast.addNewAttribute();
				at.setName("urn:unicore:attrType:defaultRole");
				p = XmlString.Factory.newInstance();
				p.setStringValue("user");
				at.addNewAttributeValue().set(p);

				// groups
				at = ast.addNewAttribute();
				at.setName("urn:unicore:attrType:group");
				p = XmlString.Factory.newInstance();
				p.setStringValue("users");
				at.addNewAttributeValue().set(p);
				p.setStringValue("hpc");
				at.addNewAttributeValue().set(p);

				// non-mapped attribute
				at = ast.addNewAttribute();
				at.setName("urn:some:attrType:someName");
				p = XmlString.Factory.newInstance();
				p.setStringValue("someValue");
				at.addNewAttributeValue().set(p);
				
			}
		}

		public String createSoapReply(ResponseDocument rd) throws Exception {
			return "<?xml version = \"1.0\"?>"+
			"<se:Envelope xmlns:se =\"http://schemas.xmlsoap.org/soap/envelope/\">"+
			"<se:Body>"+rd.toString()+"</se:Body></se:Envelope>";
		}
	}

}
