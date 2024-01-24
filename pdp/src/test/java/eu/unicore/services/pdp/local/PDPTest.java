package eu.unicore.services.pdp.local;

import org.junit.Before;

import eu.unicore.services.pdp.AbstractPDPTest;


public class PDPTest extends AbstractPDPTest
{
	@Before
	public void setUp() throws Exception
	{
		String f = "src/test/resources/local/pdp2.conf";
		pdp = new LocalHerasafPDP();
		((LocalHerasafPDP)pdp).initialize(f, "http://test123.local");
		((LocalHerasafPDP)pdp).lps.reload(f);
	}
}
