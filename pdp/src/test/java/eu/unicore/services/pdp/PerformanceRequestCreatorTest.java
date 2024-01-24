package eu.unicore.services.pdp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.herasaf.xacml.core.context.RequestMarshaller;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.junit.Test;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.pdp.request.creator.HerasafXacml2RequestCreator;
import eu.unicore.services.pdp.request.creator.XmlbeansXacml2RequestCreator;
import eu.unicore.services.pdp.request.profile.EMI1Profile;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.ResourceDescriptor;
import xmlbeans.oasis.xacml.x2.x0.context.RequestDocument;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLAuthzDecisionQueryDocument;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLAuthzDecisionQueryType;

public class PerformanceRequestCreatorTest {
	private final int N = 100000;
	private final int S = 2;

	@Test
	public void testFake() {} //to keep surefire quiet

	//@Test
	public void test() {
		long t1 = 0;
		long t2 = 0;
		long t3 = 0;
		long t4 = 0;

		try {

			System.out.println("Request number:" + N + "\nRepetitions number:"
					+ S + "\n");

			SimplePDPFactory.getSimplePDP();

			String baseUrl = "https://localhost:7777/TEST-SRV";

			XmlbeansXacml2RequestCreator beanscreator = new XmlbeansXacml2RequestCreator(
					new EMI1Profile(baseUrl));

			HerasafXacml2RequestCreator hersafcreator = new HerasafXacml2RequestCreator(
					new EMI1Profile(baseUrl));

			Client c = MockAuthZContext.createRequest("admin",
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"http://serviceName", "default_resource",
					"CN=Testing Owner,C=XX");

			for (int j = 0; j < S; j++) {

				long sT = System.currentTimeMillis();
				for (int i = 0; i < N; i++) {
					beanscreator.createRequest(c, action, des);
					// System.out.println(d.xmlText(new
					// XmlOptions().setSavePrettyPrint()));
				}

				long eT = System.currentTimeMillis();

				t1 = t1 + (eT - sT);

				sT = System.currentTimeMillis();
				for (int i = 0; i < N; i++) {
					RequestType r = hersafcreator.createRequest(c, action, des);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					RequestMarshaller.marshal(r,baos);
					RequestDocument doc = RequestDocument.Factory.parse(baos
							.toString());
					XACMLAuthzDecisionQueryDocument ddoc = XACMLAuthzDecisionQueryDocument.Factory
							.newInstance();
					XACMLAuthzDecisionQueryType q = XACMLAuthzDecisionQueryType.Factory
							.newInstance();
					q.setRequest(doc.getRequest());
					NameID nn = new NameID("myurl",
							SAMLConstants.NFORMAT_ENTITY);
					q.setIssuer(nn.getXBean());
					ddoc.setXACMLAuthzDecisionQuery(q);
				}
				eT = System.currentTimeMillis();

				t2 = t2 + (eT - sT);

				sT = System.currentTimeMillis();
				for (int i = 0; i < N; i++) {
					hersafcreator.createRequest(c, action, des);
				}
				eT = System.currentTimeMillis();

				t3 = t3 + (eT - sT);
				
				sT = System.currentTimeMillis();
				long foo=0;
				for (int i = 0; i < N; i++) {
					XACMLAuthzDecisionQueryDocument r = beanscreator
							.createRequest(c, action, des);
					RequestDocument doc = RequestDocument.Factory.newInstance();
					doc.setRequest(r.getXACMLAuthzDecisionQuery().getRequest());
					InputStream is = new ByteArrayInputStream(doc.xmlText().getBytes());
					foo+=RequestMarshaller.unmarshal(is).hashCode(); //prevent JIT optimization
				}
				eT = System.currentTimeMillis();
				t4 = t4 + (eT - sT);
				if(foo!=0)System.out.println("Done");
			}
			
			System.out.println("Creation XMLBEANSXACML2 req:" + t1 / S + " ms");

			System.out.println("HERASAF to XMLBEANSXACML2 cast: " + t2 / S
					+ " ms");

			System.out.println("Creation HERASAF req:" + t3 / S + " ms");

			System.out.println("XMLBEANSXACML2 to HERASAF cast:" + t4 / S
					+ " ms");

		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}
}
