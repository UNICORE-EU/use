package eu.unicore.services.pdp.local;

import org.junit.Before;

import eu.unicore.services.pdp.AbstractPerformanceTester;

public class HerasafPerformanceCheck extends AbstractPerformanceTester
{
	@Before
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
	}
}
