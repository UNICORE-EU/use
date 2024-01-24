package eu.unicore.services.aip.integration;

import eu.unicore.security.Client;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;

public class MockPDP implements UnicoreXPDP
{
	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, 
			ResourceDescriptor d) throws Exception
	{
		if (c.getRole().getName().equals("user"))
			return new PDPResult(PDPResult.Decision.PERMIT, "");
		return new PDPResult(PDPResult.Decision.DENY, "no USER role");
	}

}
