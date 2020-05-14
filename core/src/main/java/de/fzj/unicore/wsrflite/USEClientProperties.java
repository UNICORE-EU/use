/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.wsrflite;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Simple extension of {@link ClientProperties}: sets ssl mode to the same value as containers ssl mode 
 * (passed as argument).  
 * @author K. Benedyczak
 */
public class USEClientProperties extends ClientProperties {
	@DocumentationReferencePrefix
	public static final String PREFIX = ContainerProperties.PREFIX+ClientProperties.DEFAULT_PREFIX;
	
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static 
	{
		META.putAll(ClientProperties.META);
		META.remove(PROP_SSL_ENABLED);
	}
	
	public USEClientProperties(Properties p, IContainerSecurityConfiguration baseSettings)
			throws ConfigurationException {
		super(createClientProperties(p, baseSettings), 
				PREFIX, baseSettings);
	}
	
	private static Properties createClientProperties(Properties p, IContainerSecurityConfiguration baseSettings) {
		p.setProperty(PREFIX+ClientProperties.PROP_SSL_ENABLED,	String.valueOf(baseSettings.isSslEnabled()));
		return p;
	}
}
