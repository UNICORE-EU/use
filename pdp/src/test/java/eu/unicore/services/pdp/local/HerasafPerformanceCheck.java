package eu.unicore.services.pdp.local;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;

import eu.unicore.services.pdp.AbstractPerformanceTester;
import eu.unicore.services.security.pdp.DefaultPDP;

public class HerasafPerformanceCheck extends AbstractPerformanceTester
{
	@BeforeEach
	public void setup() throws Exception
	{
		pdp = new LocalHerasafPDP();
		((LocalHerasafPDP)pdp).initialize("src/test/resources/local/pdp2.conf",
				"http://test123.local");
		threads = 5;
		iterationsPerThread = 10000;
		createStandardSetup();
	}
	
	public static void main(String []args) throws Exception
	{
		HerasafPerformanceCheck obj = new HerasafPerformanceCheck();
		obj.setup();
		obj.testUser();

		// just for fun, setup a similar test with the DefaultPDP
		System.out.println("\n *** DefaultPDP comparison***\n");
		DefPDPTester t = new DefPDPTester();
		t.setup();
		t.testUser();
	}

	public static class DefPDPTester extends AbstractPerformanceTester
	{
		public void setup() throws Exception
		{
			pdp = new DefaultPDP();
			DefaultPDP dPDP = (DefaultPDP)pdp;
			dPDP.setServiceRules("registries", Collections.singletonList(DefaultPDP.PERMIT_READ));
			dPDP.setServiceRules("registryentries", Collections.singletonList(DefaultPDP.PERMIT_READ));
			dPDP.setServiceRules("ServiceGroupEntry", Collections.singletonList(DefaultPDP.PERMIT_READ));
			dPDP.setServiceRules("Registry", Collections.singletonList(DefaultPDP.PERMIT_READ));
			threads = 5;
			iterationsPerThread = 10000;
			createStandardSetup();
		}
	}

}