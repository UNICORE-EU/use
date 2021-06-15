package eu.unicore.services.registry.ws;

import eu.unicore.services.registry.ServiceRegistryEntryImpl;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

/**
 * home of the ServiceGroup
 * 
 * @author schuller
 */
public class ServiceRegistryEntryHomeImpl extends WSResourceHomeImpl {

	@Override
	protected ServiceRegistryEntryImpl doCreateInstance() {
		return new ServiceRegistryEntryImpl();
	}

}
