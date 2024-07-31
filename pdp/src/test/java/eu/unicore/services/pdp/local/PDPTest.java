package eu.unicore.services.pdp.local;

import org.junit.jupiter.api.BeforeEach;

import eu.unicore.services.pdp.AbstractPDPTest;


public class PDPTest extends AbstractPDPTest
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
