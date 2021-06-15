package eu.unicore.uas.pdp.argus.pap.unit;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.pdp.ActionDescriptor;
import eu.unicore.services.pdp.PDPResult;
import eu.unicore.services.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.uas.pdp.MockAuthZContext;
import eu.unicore.uas.pdp.argus.pap.ArgusPAPTestBase;

/*
 * src/test/resources/argus/papserver/policy_spl/full.spl
 */
public class ArgusPermitTest extends ArgusPAPTestBase {

	@Test
	public void permitAction() {
		try {

			Client c = MockAuthZContext
					.createRequest("user",
							"C=EU, L=Testing City, O=Testing Organization, CN=localhost");
			String[] vos = { "/vo.plgrid.pl/sites/WCSS", "/ops-vo",
					"/vo.plgrid.pl/sites2/WCSS", "vo.plgrid.pl" };
			c.setVos(vos);
			ActionDescriptor action = new ActionDescriptor("PutResourcePropertyDocument", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor("serviceName",
					"BESManagement?res=any", "CN=Testing Owner,C=XX");

			System.out.println("WAITING FOR UPDATE POLICY");
			Thread.sleep(10000);

			PDPResult result2 = pap.checkAuthorisation(c, action, des);
			assertTrue(result2.getDecision().equals(Decision.PERMIT));


		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
}
