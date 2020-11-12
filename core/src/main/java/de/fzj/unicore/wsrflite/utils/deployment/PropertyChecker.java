package de.fzj.unicore.wsrflite.utils.deployment;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Used to perform additional checks for properties, usually to detect obsolete settings and/or
 * to update some values where it is possible.
 * @author K. Benedyczak
 */
public interface PropertyChecker {

	/**
	 * check supplied properties
	 * 
	 * @param props - properties to check
	 * @param logger - logger for logging warning messages
	 * @throws ConfigurationException if an unrecoverable problem with properties is found
	 */
	public void checkProperties(Properties props, Logger logger) throws ConfigurationException;
}
