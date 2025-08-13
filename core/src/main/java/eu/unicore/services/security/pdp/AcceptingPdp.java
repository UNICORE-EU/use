package eu.unicore.services.security.pdp;

import eu.unicore.security.Client;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;

/**
 * Trivial PDP always accepting everything
 * @author K. Benedyczak
 */
public class AcceptingPdp implements UnicoreXPDP {

	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, ResourceDescriptor d) throws Exception {
		return new PDPResult(Decision.PERMIT, "This PDP always grants access");
	}

}