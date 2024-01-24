package eu.unicore.services.pdp.local;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.junit.Test;

public class PolicyStoreTest
{
	
	private static class MockListener implements PolicyListener
	{
		public int modC = 0;
		public List<Evaluatable> policies;
		public String algorithm;
		public void updateConfiguration(List<Evaluatable> policies,
				String algorithm)
		{
			modC++;
			this.policies = policies;
			this.algorithm = algorithm;
		}
	};
	
	@Test
	public void test()
	{
		try
		{
			new File("target/test-classes/local/xacml/").mkdirs();
			File p2 = new File("target/test-classes/local/xacml/zzpolicy2.xml");
			if (p2.exists())
				p2.delete();
			SimplePDPFactory.getSimplePDP();
			MockListener pl = new MockListener();
			new LocalPolicyStore(pl, "target/test-classes/local/pdp.conf");
			assertTrue(pl.modC == 1);
			assertTrue(pl.algorithm.equals("urn:oasis:names:tc:xacml:1.1:policy-combining-algorithm:ordered-permit-overrides"));
			assertTrue(pl.policies.size() == 1);
			String policyId = pl.policies.get(0).getId().toString();
			assertTrue(policyId.equals("testPolicy:default"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Got unexpected exception" + e);
		}
	}
}
