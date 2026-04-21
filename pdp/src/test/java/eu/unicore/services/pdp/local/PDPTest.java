package eu.unicore.services.pdp.local;

import org.junit.jupiter.api.BeforeEach;


public class PDPTest extends LocalPDPTest
{
	@BeforeEach
	public void setUp() throws Exception
	{
		String f = "src/test/resources/local/pdp2.conf";
		pdp = new LocalHerasafPDP();
		((LocalHerasafPDP)pdp).initialize(f, "http://test123.local");
		((LocalHerasafPDP)pdp).lps.reload(f);
	}
}
