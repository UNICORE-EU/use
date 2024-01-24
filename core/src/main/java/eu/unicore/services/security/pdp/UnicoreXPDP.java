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
public interface UnicoreXPDP extends KernelInjectable {

	public default void setKernel(Kernel k) {}

	public PDPResult checkAuthorisation(Client c, ActionDescriptor action, ResourceDescriptor d) 
		throws Exception;
}
