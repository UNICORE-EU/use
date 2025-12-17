package eu.unicore.services.rest.security;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.proto.AssertionResponse;
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
import jakarta.ws.rs.core.Application;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.AttributeStatementType;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.assertion.AudienceRestrictionType;
import xmlbeans.org.oasis.saml2.assertion.AuthnStatementType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class MockSAMLServer {

	private Kernel kernel;
	static String url;
	static boolean sign = false;
	static String audience = "http://localhost:55333";

	public MockSAMLServer(boolean sign)throws Exception{
		MockSAMLServer.sign = sign;
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55444");
		p.setProperty("persistence.directory", "target/data");
		p.setProperty("container.security.credential.format", "jks");
		p.setProperty("container.security.credential.path", "src/test/resources/keystore.jks");
		p.setProperty("container.security.credential.password", "the!njs");
		p.setProperty("container.security.truststore.type", "directory");
		p.setProperty("container.security.truststore.directoryLocations.1", "src/test/resources/cacert.pem");
		kernel = new Kernel(p);
		kernel.start();			
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyIDPApplication.class);
		dd.setName("idp");
		dd.setKernel(kernel);
		kernel.getDeploymentManager().deployService(dd);
		JettyServer server=kernel.getServer();
		url = server.getUrls()[0].toExternalForm()+"/rest";
		System.out.println("******* MOCK-SAML SERVER STARTED.");
	}

	public void stopServer()throws Exception{
		kernel.shutdown();
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

		@POST
		@Path("/saml")
		@Consumes("*/*")
		@Produces("application/xml")
		public String getRepresentation(String xml) throws Exception {
			Document soapenv = (Document)XmlObject.Factory.parse(xml).newDomNode();
			NodeList nl = soapenv.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "AuthnRequest");
			AuthnRequestDocument ard = AuthnRequestDocument.Factory.parse(nl.item(0));
			NameIDType issuer = NameIDType.Factory.newInstance();
			issuer.setStringValue(kernel.getContainerSecurityConfiguration().
					getCredential().getSubjectName());
			Assertion assertion = new Assertion();
			AssertionType ass1 = assertion.getXMLBean();
			ass1.setID("foo_"+System.currentTimeMillis());
			ass1.setIssueInstant(Calendar.getInstance());
			ass1.setVersion(SAMLConstants.SAML2_VERSION);
			ass1.setIssuer(issuer);
			addAuthnStatement(ass1, ard.getAuthnRequest().getID(), "CN=demouser,OU=saml");
			addAttributeStatement(ass1);
			if(sign) {
				X509Credential cred = kernel.getContainerSecurityConfiguration().getCredential();
				assertion.sign(cred.getKey(), cred.getCertificateChain());
			}
			AssertionResponse rr = new AssertionResponse(issuer, ard.getAuthnRequest().getID());
			rr.addAssertion(assertion);
			return idpReply(rr.getXMLBeanDoc());
		}

		private void addAuthnStatement(AssertionType assertion, String responseTo, String userDN) throws Exception {
			AuthnStatementType at = assertion.addNewAuthnStatement();
			at.setAuthnInstant(Calendar.getInstance());
			AudienceRestrictionType aud = assertion.addNewConditions().addNewAudienceRestriction();
			aud.addAudience(audience);
			Calendar notOnOrAfter = Calendar.getInstance();
			notOnOrAfter.setTimeInMillis(System.currentTimeMillis()+100000);
			assertion.getConditions().setNotOnOrAfter(notOnOrAfter);
			NameIDType subj = NameIDType.Factory.newInstance();
			subj.setFormat(SAMLConstants.NFORMAT_DN);
			subj.setStringValue(userDN);
			assertion.addNewSubject().setNameID(subj);
			SubjectConfirmationType sct = assertion.getSubject().addNewSubjectConfirmation();
			sct.setMethod(SAMLConstants.CONFIRMATION_BEARER);
			SubjectConfirmationDataType confData = sct.addNewSubjectConfirmationData();
			confData.setRecipient(audience);
			confData.setNotOnOrAfter(notOnOrAfter);
			confData.setInResponseTo(responseTo);
		}

		private void addAttributeStatement(AssertionType assertion) throws Exception {
			AttributeStatementType ast = assertion.addNewAttributeStatement();
			AttributeType at = ast.addNewAttribute();
			at.setName("preferredUsername");
			XmlString p = XmlString.Factory.newInstance();
			p.setStringValue("demouser");
			at.addNewAttributeValue().set(p);
		}

		public String idpReply(ResponseDocument rd) throws Exception {
			return "<?xml version = \"1.0\"?>"+
			"<se:Envelope xmlns:se =\"http://schemas.xmlsoap.org/soap/envelope/\">"+
			"<se:Body>"+rd.xmlText()+"</se:Body></se:Envelope>";
		}
	}

}