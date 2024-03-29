package eu.unicore.services.registry;

import java.util.Calendar;
import java.util.Map;

import eu.unicore.services.InitParameters;

public class RegistryEntryInitParameters extends InitParameters {

	public RegistryEntryInitParameters() {
		super();
	}

	public RegistryEntryInitParameters(Calendar terminationTime) {
		super(null, terminationTime);
	}

	public RegistryEntryInitParameters(String uuid, Calendar terminationTime) {
		super(uuid, terminationTime);
	}

	public RegistryEntryInitParameters(String uuid) {
		super(uuid);
	}

	public Map<String,String> content;
}
