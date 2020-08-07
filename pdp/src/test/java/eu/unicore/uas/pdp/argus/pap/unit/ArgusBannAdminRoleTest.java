package eu.unicore.uas.pdp.argus.pap.unit;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult.Decision;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.uas.pdp.MockAuthZContext;
import eu.unicore.uas.pdp.argus.pap.ArgusPAPTestBase;
/*
 * src/test/resources/argus/papserver/policy_spl/adminRoleBann.spl
 */
public class ArgusBannAdminRoleTest extends ArgusPAPTestBase {
	@Test
	public void bannAdminRoleTest() {
		try {

			Client c = MockAuthZContext
					.createRequest("admin",
							"C=EU, L=Testing City, O=Testing Organization, CN=localhost");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor("serviceName",
					"default_resource", "CN=Testing Owner,C=XX");

			
			System.out.println("WAITING FOR UPDATE POLICY");
			Thread.sleep(10000);

			PDPResult result2 = pap.checkAuthorisation(c, action, des);
			assertTrue(result2.getDecision().equals(Decision.DENY));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	
}