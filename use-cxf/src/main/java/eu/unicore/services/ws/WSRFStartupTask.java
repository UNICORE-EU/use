/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.ws;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.impl.DefaultHome;
import de.fzj.unicore.wsrflite.server.AbstractStartupTask;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryCreator;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryHandler;
import de.fzj.unicore.wsrflite.xmlbeans.sg.ServiceGroupEntry;
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
