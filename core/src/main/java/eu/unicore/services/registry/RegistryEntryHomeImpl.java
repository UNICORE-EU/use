package eu.unicore.services.registry;

import eu.unicore.services.impl.DefaultHome;

public class RegistryEntryHomeImpl extends DefaultHome {
	
	protected RegistryEntryImpl doCreateInstance(){
		return new RegistryEntryImpl();
	}

}
