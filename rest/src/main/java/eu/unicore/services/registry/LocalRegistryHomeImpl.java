package eu.unicore.services.registry;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class LocalRegistryHomeImpl extends DefaultHome {

	@Override
	protected Resource doCreateInstance() {
		return new LocalRegistryImpl();
	}

}
