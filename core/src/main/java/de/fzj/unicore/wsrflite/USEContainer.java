package de.fzj.unicore.wsrflite;

import java.util.Properties;

/**
 * for backwards compatibility of start scripts
 */
public class USEContainer extends eu.unicore.services.USEContainer {
	
	public USEContainer(Properties config, String name) throws Exception {
		super(config, name);
	}

	public USEContainer(String configFile, String name) throws Exception {
		super(configFile, name);
	}
}
