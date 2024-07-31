package eu.unicore.services.pdp.local;

import org.junit.jupiter.api.BeforeEach;

import eu.unicore.services.pdp.AbstractPerformanceTester;

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
	}
}
