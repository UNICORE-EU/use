package de.fzj.unicore.wsrflite.registry;

import de.fzj.unicore.wsrflite.impl.BaseModel;

public class ServiceRegistryEntryModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	private String endpoint;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public String getFrontend(String serviceType) {
		return "de.fzj.unicore.wsrflite.registry.ws.SGEFrontend";
	}
}
