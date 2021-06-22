package eu.unicore.services.registry;

import eu.unicore.services.impl.DefaultHome;

public class RegistryHomeImpl extends DefaultHome {

	@Override
	protected RegistryImpl doCreateInstance() {
		return new RegistryImpl();
	}

}
