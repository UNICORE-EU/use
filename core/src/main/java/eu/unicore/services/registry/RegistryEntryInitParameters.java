package eu.unicore.services.registry;

import java.util.Calendar;
import java.util.Map;

import eu.unicore.services.InitParameters;

public class RegistryEntryInitParameters extends InitParameters {

	public RegistryEntryInitParameters(Calendar terminationTime) {
		super(null, terminationTime);
	}

	public Map<String,String> content;
}
