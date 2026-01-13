package eu.unicore.services.messaging.impl;

import eu.unicore.services.messaging.Message;

public class ResourceAddedMessage extends Message {

	private static final long serialVersionUID = 1L;

	private final String instanceUUID;
	private final String serviceName;

	public ResourceAddedMessage(String instanceUUID, String serviceName) {
		super();
		this.instanceUUID = instanceUUID;
		this.serviceName = serviceName;
	}

	public String getAddedInstance() {
		return instanceUUID;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String toString() {
			return "added: "+instanceUUID+"@"+serviceName;
	}
}