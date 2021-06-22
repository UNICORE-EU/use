package eu.unicore.services.registry.ws;

import org.oasisOpen.docs.wsrf.sg2.ServiceGroupEntryRPDocument;

import eu.unicore.services.registry.RegistryEntryImpl;
import eu.unicore.services.ws.impl.WSRFFrontend;

public class SGEFrontend extends WSRFFrontend {

	RegistryEntryImpl resource;
	
	public SGEFrontend(RegistryEntryImpl r) {
		super(r, ServiceGroupEntryRPDocument.type.getDocumentElementName(), null);
		this.resource = r;
	}
	
}
