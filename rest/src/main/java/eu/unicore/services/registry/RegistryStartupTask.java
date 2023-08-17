/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.registry;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.util.Log;


/**
 * Creates registry instance and sets up Registry-related tasks
 * 
 * @author K. Benedyczak
 */
public class RegistryStartupTask implements Runnable {
	
	private static final Logger logger=Log.getLogger(Log.UNICORE, RegistryStartupTask.class);
	
	private final Kernel kernel;
	
	public RegistryStartupTask(Kernel kernel) {
		this.kernel = kernel;
	}
	
	public void run() {
		try {
			RegistryCreator registryCreator = RegistryCreator.get(kernel);
			registryCreator.createRegistry();
			RegistryHandler registryHandler = RegistryHandler.get(kernel);
			if (!registryCreator.isGlobalRegistry()) {
				registryCreator.refreshRegistryEntries();
			}
			setupRegistryCrawler();
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void setupRegistryCrawler(){
		Runnable command = new Runnable(){
			public void run(){
				try{
					RegistryHandler h = kernel.getAttribute(RegistryHandler.class);
					h.updatePublicKeys();
				}catch(Throwable ex){
					Log.logException("", ex, Log.getLogger(Log.UNICORE, RegistryHandler.class));
				}
			}
		};
		command.run();
		kernel.getContainerProperties().getThreadingServices().
		getScheduledExecutorService().scheduleAtFixedRate(command, 60, 60, TimeUnit.SECONDS);
	}
}
