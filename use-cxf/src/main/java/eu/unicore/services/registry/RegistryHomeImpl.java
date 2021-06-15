package eu.unicore.services.registry;

import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class RegistryHomeImpl extends WSResourceHomeImpl {

	@Override
	protected RegistryImpl doCreateInstance() {
		return new RegistryImpl();
	}

}
