package de.fzj.unicore.wsrflite.registry.ws;

import de.fzj.unicore.wsrflite.registry.ServiceRegistryEntryImpl;
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
