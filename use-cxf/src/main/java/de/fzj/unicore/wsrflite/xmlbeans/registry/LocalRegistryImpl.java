package de.fzj.unicore.wsrflite.xmlbeans.registry;

import java.util.Calendar;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.xmlbeans.client.ExternalRegistryClient;

import eu.unicore.util.Log;


/**
 * Local Registry. <br>
 * 
 * Entries in the local registry can be pushed to one or more external registries,
 * with a simple update/refresh mechanism. The lifetime of a local registry entry is determined 
 * by the smallest termination time returned by the external registries minus a grace period (60 secs).
 * In case the external registries fail to respond, the lifetime supplied to Add() is used, or 
 * as fallback, a fixed interval of 5 minutes. This mechanism ensures that the external registries are 
 * in a reasonably up-to-date state.
 *  
 * 
 * @author demuth
 * @author schuller
 */
public class LocalRegistryImpl extends RegistryImpl {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", LocalRegistryImpl.class);
	
	/**
	 * push entry to any configured external registries.
	 * The lifetime of the new entry is determined by the external registry, or 
	 * the time supplied to this method, or a 5 minute fallback.
	 */
	@Override
	protected Calendar pushToExternalRegistries(String endpoint, Map<String,String>content, Calendar requestedTT) {
		RegistryHandler rh=kernel.getAttribute(RegistryHandler.class);
		if(rh!=null && rh.usesExternalRegistry()){
			try {
				ExternalRegistryClient externalRegistryClient = rh.getExternalRegistryClient();
				requestedTT = externalRegistryClient.addRegistryEntry(endpoint, content);
				if(logger.isDebugEnabled()){
					logger.debug("Will try to re-add entry for "
							+ endpoint 
							+ " at: "+requestedTT.getTime().toString());
				}
			} catch (Exception e) {
				Log.logException("Could not connect to external Registry!",e,logger);
			}
		}
		return requestedTT;
	}
	
	
	@Override
	public Calendar getDefaultEntryLifetime() {
		Calendar tt = Calendar.getInstance();
		tt.add(Calendar.MINUTE, 5);
		return tt;
	}
}
