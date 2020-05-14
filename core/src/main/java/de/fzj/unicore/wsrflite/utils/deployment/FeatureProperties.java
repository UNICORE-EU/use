package de.fzj.unicore.wsrflite.utils.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.ContainerProperties;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

public class FeatureProperties extends PropertiesHelper {
	
	protected static final Logger log = Log.getLogger(Log.CONFIGURATION, FeatureProperties.class);

	
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static 
	{
		META.put("enable", new PropertyMD("true").setBoolean().
				setDescription("Controls whether the feature is enabled."));
	}
	
	public FeatureProperties(String featureName, ContainerProperties source) {
		this(featureName, source.getRawProperties());
	}

	public FeatureProperties(String featureName, Properties properties) {
		super("container.feature."+featureName+".", properties, META, log);
	}

	public boolean isEnabled(){
		return getBooleanValue("enable");
	}
	
}
