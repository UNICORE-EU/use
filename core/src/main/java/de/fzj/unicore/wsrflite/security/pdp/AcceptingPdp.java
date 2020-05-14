/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.wsrflite.security.pdp;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult.Decision;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
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