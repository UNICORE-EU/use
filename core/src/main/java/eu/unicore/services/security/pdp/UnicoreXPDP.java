/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 25-10-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.services.security.pdp;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.security.util.ResourceDescriptor;

/**
 * A generic PDP interface.
 * 
 * @author golbi
 */
public interface UnicoreXPDP extends KernelInjectable
{

	public default void setKernel(Kernel k) {}

	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, ResourceDescriptor d) 
		throws Exception;
}
