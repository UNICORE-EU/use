/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2010-11-14
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.integration;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.pdp.ActionDescriptor;
import eu.unicore.services.pdp.PDPResult;
import eu.unicore.services.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.util.httpclient.IClientConfiguration;

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

	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws Exception {
	}
}
