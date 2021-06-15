/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 02-11-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp.local;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.junit.Test;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ThreadingServices;

public class PolicyStoreTest
{
	private static final String TEST_POLICY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
	"<Policy xmlns=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\"" +
	" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
	" xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os http://docs.oasis-open.org/xacml/access_control-xacml-2.0-policy-schema-os.xsd\""+
	" PolicyId=\"testPolicy:new\"" +
	" RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.1:rule-combining-algorithm:ordered-permit-overrides\">" +
	"	<Target></Target>" +
	"</Policy>";
	
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
			ThreadingServices ts = new ContainerProperties(new Properties(), false).getThreadingServices();
			new LocalPolicyStore(pl, "target/test-classes/local/pdp.conf", 50, ts);
			assertTrue(pl.modC == 1);
			assertTrue(pl.algorithm.equals("urn:oasis:names:tc:xacml:1.1:policy-combining-algorithm:ordered-permit-overrides"));
			assertTrue(pl.policies.size() == 1);
			String policyId = pl.policies.get(0).getId().toString();
			assertTrue(policyId.equals("testPolicy:default"));
			
			BufferedOutputStream fos = new BufferedOutputStream(
					new FileOutputStream(p2.getAbsolutePath()));
			fos.write(TEST_POLICY.getBytes());
			fos.flush();
			fos.close();
			Thread.sleep(1000);
			File f = new File("target/test-classes/local/pdp.conf");
			f.setLastModified(System.currentTimeMillis());
			Thread.sleep(2000);
			
			assertTrue(pl.modC == 2);
			assertTrue(pl.algorithm.equals("urn:oasis:names:tc:xacml:1.1:policy-combining-algorithm:ordered-permit-overrides"));
			assertTrue(pl.policies.size() == 2);
			policyId = pl.policies.get(0).getId().toString();
			assertTrue(policyId.equals("testPolicy:default"));
			policyId = pl.policies.get(1).getId().toString();
			assertTrue(policyId.equals("testPolicy:new"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Got unexpected exception" + e);
		}
	}
}
