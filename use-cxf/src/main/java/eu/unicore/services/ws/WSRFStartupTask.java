/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.ws;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.registry.RegistryCreator;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.server.AbstractStartupTask;
import eu.unicore.services.ws.sg.ServiceGroupEntry;
import eu.unicore.util.Log;


/**
 * Initialization of the WSRF specific USE part.
 * Creates registry (if needed) and registry clients
 * 
 * @author K. Benedyczak
 */
public class WSRFStartupTask extends AbstractStartupTask {
	
	private static final Logger logger=Log.getLogger(Log.UNICORE, WSRFStartupTask.class);
	
	public void run() throws Exception {
		Kernel kernel = getKernel();
		RegistryCreator registryCreator = new RegistryCreator(kernel);
		kernel.setAttribute(RegistryCreator.class, registryCreator);
		registryCreator.createRegistry();
		
		RegistryHandler registryHandler = new RegistryHandler(kernel);
		kernel.setAttribute(RegistryHandler.class, registryHandler);
		
		if (!registryCreator.isGlobalRegistry())
			forceRefreshRegistryEntries();
		
		setupRegistryCrawler();
	}
	

	private void forceRefreshRegistryEntries() {
		try {
			Home home = getKernel().getHome(ServiceGroupEntry.SERVICENAME);
			if (home != null && home instanceof DefaultHome) {
				((DefaultHome) home).runExpiryCheckNow();
			}
		} catch (Exception ex) {
			Log.logException("Error running expiry checks for <"
					+ ServiceGroupEntry.SERVICENAME + ">", ex, logger);
		}
	}
	
	private void setupRegistryCrawler(){
		Kernel kernel = getKernel();
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
