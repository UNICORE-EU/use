/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package eu.unicore.services;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.utils.deployment.PropertyChecker;

/**
 * Checks obsolete USE properties and updates some obsolete values.
 * @author K. Benedyczak
 */
public class USEPropertyChecker implements PropertyChecker {
	
	@Override
	public void checkProperties(Properties props, Logger logger) {
		
	}
}
