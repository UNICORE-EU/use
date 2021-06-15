package eu.unicore.services.registry;

import java.util.Calendar;

import eu.unicore.services.InitParameters;

public class ServiceRegistryEntryInitParameters extends InitParameters {

	public ServiceRegistryEntryInitParameters() {
		super();
	}

	public ServiceRegistryEntryInitParameters(Calendar terminationTime) {
		super(null, terminationTime);
	}

	public ServiceRegistryEntryInitParameters(String uuid, Calendar terminationTime) {
		super(uuid, terminationTime);
	}

	public ServiceRegistryEntryInitParameters(String uuid) {
		super(uuid);
	}

	public String endpoint;

}
