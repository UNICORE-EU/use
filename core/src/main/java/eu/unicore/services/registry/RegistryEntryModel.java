package eu.unicore.services.registry;

import eu.unicore.services.impl.BaseModel;

public class RegistryEntryModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	private String endpoint;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

}
