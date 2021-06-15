package eu.unicore.uas.pdp.argus.pap.unit;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.uas.pdp.MockAuthZContext;
import eu.unicore.uas.pdp.argus.pap.ArgusPAPTestBase;

/*
 * src/test/resources/argus/papserver/policy_spl/subjectBann.spl
 */
public class ArgusSubjectBannTest extends ArgusPAPTestBase {
	@Test
	public void subjectBannTest() {
		try {
			System.out.println("WAITING FOR UPDATE POLICY");

			Thread.sleep(10000);

			Client c = MockAuthZContext
					.createRequest("user",
							"C=EU, L=Testing City, O=Testing Organization, CN=localhost");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor("serviceName",
					"default_resource", "CN=Testing Owner,C=XX");

			PDPResult result = pap.checkAuthorisation(c, action, des);
			assertTrue(result.getDecision().equals(Decision.DENY));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
}
