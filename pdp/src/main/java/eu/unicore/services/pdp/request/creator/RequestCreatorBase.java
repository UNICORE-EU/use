package eu.unicore.services.pdp.request.creator;

import eu.unicore.security.Client;
import eu.unicore.services.pdp.request.profile.XACMLProfile;

public class RequestCreatorBase {

	protected XACMLProfile profile;

	protected boolean allowAnonymous = true;
	
	public RequestCreatorBase(XACMLProfile p) {
		profile = p;
	}
	
	public void setAllowAnonymous(boolean allowAnonymous){
		this.allowAnonymous = allowAnonymous;
	}
	
	protected void validateClient(Client c) throws IllegalArgumentException {
		if (c.getType() == Client.Type.LOCAL)
			throw new IllegalArgumentException(
					"Can not perform authorization of an local client, this is a bug");
		if (c.getDistinguishedName() == null)
			throw new IllegalArgumentException(
					"Subject DN is not available in authZ subsystem");
		if (c.getRole() == null)
			throw new IllegalArgumentException(
					"Subject's role is not available in authZ subsystem");
		if (c.getRole().getName() == null)
			throw new IllegalArgumentException(
					"Subject's role name is not available in authZ subsystem");
		
		if (!allowAnonymous && c.getType() == Client.Type.ANONYMOUS)
			throw new IllegalArgumentException(
					"Can not perform authorization of an anonymous client");
	}
}
