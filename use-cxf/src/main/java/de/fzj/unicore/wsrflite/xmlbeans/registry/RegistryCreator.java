/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.wsrflite.xmlbeans.registry;

import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.persist.impl.LockSupport;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.util.Log;

/**
 * Initializes the singleton instance of the registry service. Depending on the deployed
 * Registry home implementation it is either global or local registry.
 * 
 * @author K. Benedyczak
 * @author B. Schuller
 */
public class RegistryCreator {
	
	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryCreator.class);
	
	public static final String DEFAULT_REGISTRY_ID = "default_registry";
	
	private final boolean isGlobalRegistry; 
	
	private final Kernel kernel;
	
	public RegistryCreator(Kernel kernel) {
		this.kernel = kernel;
		Home regHome=kernel.getHome(Registry.REGISTRY_SERVICE);
		if (regHome !=null && regHome instanceof RegistryHomeImpl){
			isGlobalRegistry = true;
		}
		else{
			isGlobalRegistry = false;
		}
	}
	
	public boolean isGlobalRegistry() {
		return isGlobalRegistry;
	}
	
	public void createRegistry() throws PersistenceException {
		Home regHome=kernel.getHome(Registry.REGISTRY_SERVICE);
		if(regHome==null) {
			logger.info("No Registry service configured for this site.");
			return;
		}
		LockSupport ls=kernel.getPersistenceManager().getLockSupport();
		Lock regLock=ls.getOrCreateLock(RegistryHandler.class.getName());
		if(regLock.tryLock()){
			try{
				//check if default registry already exists
				try{
					regHome.get(DEFAULT_REGISTRY_ID);
					logger.debug("Registry has already been set up.");
				}catch(ResourceUnknownException e){
					try {
						logger.debug("Trying to start up registry.");
						createRegistryInstance(regHome);
					} catch (Exception ex) {
						logger.warn("Could not start up registry!",ex);
					}
				}
			}
			finally{
				regLock.unlock();
			}
		}
	}
	
	private void createRegistryInstance(Home regHome)throws ResourceNotCreatedException{
		InitParameters init = new InitParameters(DEFAULT_REGISTRY_ID, TerminationMode.NEVER);
		regHome.createResource(init);
		logger.debug("Added '"+DEFAULT_REGISTRY_ID+"' resource to service "+Registry.REGISTRY_SERVICE);
	}

}
