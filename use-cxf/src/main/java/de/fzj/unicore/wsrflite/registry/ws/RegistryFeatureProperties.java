package de.fzj.unicore.wsrflite.registry.ws;

import java.util.Map;
import java.util.Properties;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.utils.deployment.FeatureProperties;
import eu.unicore.util.configuration.PropertyMD;

public class RegistryFeatureProperties extends FeatureProperties {

	public static enum mode {
		local, shared
	}
	
	static 
	{
		FeatureProperties.META.put("mode", new PropertyMD().setEnum(mode.local).
				setDescription("Controls whether the Registry operates on 'local' or 'shared' mode."));
	}

	public final static Map<String, PropertyMD> META = FeatureProperties.META;
	
	public RegistryFeatureProperties(String featureName, ContainerProperties source) {
		super(featureName, source);
	}

	public RegistryFeatureProperties(String featureName, Properties properties) {
		super(featureName, properties);
	}

}