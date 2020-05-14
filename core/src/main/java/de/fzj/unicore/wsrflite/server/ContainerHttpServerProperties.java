/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package de.fzj.unicore.wsrflite.server;

import java.util.Map;
import java.util.Properties;

import de.fzj.unicore.wsrflite.ContainerProperties;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Configures USE prefix.
 * @author K. Benedyczak
 */
public class ContainerHttpServerProperties extends HttpServerProperties {
	
	@DocumentationReferencePrefix
	public static final String PREFIX = ContainerProperties.PREFIX + HttpServerProperties.DEFAULT_PREFIX;
	
	@DocumentationReferenceMeta
	protected final static Map<String, PropertyMD> defaults=HttpServerProperties.defaults;

	public ContainerHttpServerProperties(Properties source) {
		super(source, PREFIX, defaults);
	}
}
