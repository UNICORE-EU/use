/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2010-11-14
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult;
import de.fzj.unicore.wsrflite.security.pdp.UnicoreXPDP;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.util.httpclient.IClientConfiguration;

public class MockPDP implements UnicoreXPDP
{
	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, 
			ResourceDescriptor d) throws Exception
	{
		if (!c.getRole().getName().equals("user"))
			return new PDPResult(PDPResult.Decision.DENY, "No user role");
			
		String[] vos = c.getVos();
		if (c.getVos().length == 0)
			return new PDPResult(PDPResult.Decision.DENY, "0 VOs found");
		
		for (String s: vos)
			if ("/UUDB/SiteA".equals(s))
				return new PDPResult(PDPResult.Decision.PERMIT, "");
		return new PDPResult(PDPResult.Decision.DENY, "right VO not found");
	}

	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws Exception {
	}
}
