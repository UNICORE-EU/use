/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.security.pdp;

import eu.unicore.security.Client;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Trivial PDP always accepting everything
 * @author K. Benedyczak
 */
public class AcceptingPdp implements UnicoreXPDP {
	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws Exception {
	}

	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, ResourceDescriptor d) throws Exception {
		return new PDPResult(Decision.PERMIT, "This PDP always grants access");
	}
	
}