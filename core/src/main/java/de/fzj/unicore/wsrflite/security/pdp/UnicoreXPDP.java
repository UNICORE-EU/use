/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 25-10-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.wsrflite.security.pdp;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * A generic PDP interface.
 * 
 * @author golbi
 */
public interface UnicoreXPDP
{
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration, 
			IClientConfiguration clientConfiguration) throws Exception;
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, ResourceDescriptor d) 
		throws Exception;
}
