package eu.unicore.services.messaging.impl;

import eu.unicore.services.messaging.Message;

public class ResourceDeletedMessage extends Message {

	private static final long serialVersionUID = 1L;

	private final String instanceUUID;
	private final String serviceName;

	public ResourceDeletedMessage(String instanceUUID, String serviceName) {
		super();
		this.instanceUUID = instanceUUID;
		this.serviceName = serviceName;
	}

	public String getDeletedInstance() {
		return instanceUUID;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String toString() {
			return "deleted: "+instanceUUID+"@"+serviceName;
	}
}