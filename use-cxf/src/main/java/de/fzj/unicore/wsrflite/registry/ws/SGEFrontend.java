package de.fzj.unicore.wsrflite.registry.ws;

import org.oasisOpen.docs.wsrf.sg2.ServiceGroupEntryRPDocument;

import de.fzj.unicore.wsrflite.registry.ServiceRegistryEntryImpl;
import eu.unicore.services.ws.impl.WSRFFrontend;

public class SGEFrontend extends WSRFFrontend {

	ServiceRegistryEntryImpl resource;
	
	public SGEFrontend(ServiceRegistryEntryImpl r) {
		super(r, ServiceGroupEntryRPDocument.type.getDocumentElementName(), null);
		this.resource = r;
	}
	
}
