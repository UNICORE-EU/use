/*********************************************************************************
   * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package eu.unicore.services.registry;

import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.impl.InstanceChecker;
import eu.unicore.util.Log;

/**
 * Checks and refreshes a registry entry in the local registry.
 * 
 * @author demuth
 * @author schuller
 */
public class RegistryEntryUpdater implements InstanceChecker {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryEntryUpdater.class);
	
	public boolean check(Home home, String id)throws ResourceUnknownException,PersistenceException{
		Calendar c=home.getTerminationTime(id);
		logger.debug("Checking <{} {}> TT = {}", home.getServiceName(), id, (c!=null?c.getTime():"none"));
		//if for some reason the TT is null, force a refresh (in contrast to the usual expiry check)
		return c==null? true : (c.compareTo(Calendar.getInstance())<=0);
	}

	/**
	 * For the current registry entry, it is checked whether the 
	 * corresponding member service (i.e. the service registered in the local registry)
	 * is still alive. 
	 * 
	 * <ul>
	 *  <li>If the member service is no longer alive, its entry is refreshed, i.e. re-added 
	 * to the local registry.
	 *
	 *  <li>If the member service is gone, the registry entry is removed.
	 *</ul>
	 *
	 * @return <code>true</code> if the registry entry is still valid, <code>false</code> 
	 * if the service is gone and the registry entry was removed 
	 */
	@Override
	public boolean process(Home home, String id) {
		if(home.isShuttingDown())return true;
		Kernel kernel=home.getKernel();
		String serviceName = home.getServiceName();
		try{
			RegistryEntryImpl entry=(RegistryEntryImpl)home.get(id);
			String memberAddress = entry.getModel().getEndpoint();
			
			//check that URL is still basically correct (e.g. hostname, port) 
			if(!checkBasicCorrectness(memberAddress, kernel)){
				logger.info("Member address <{}> is no longer valid, destroying registry entry.", memberAddress);
				entry.destroy();
				home.destroyResource(id);
				//instance is invalid and should be removed from all checks
				return false;
			}
			try
			{
				reAdd(home.getKernel(),memberAddress);
				logger.debug("Refreshed registry entry for: {}", memberAddress);
			}
			catch(Exception e)
			{						
				Log.logException("Error refreshing service entry for: "+memberAddress,e,logger);
			}		
		}

		catch(Exception e){
			Log.logException("Could not update registry entry "+serviceName+"/"+id,e,logger);
		}

		// instance is still valid
		return true;
	}
	
	/**
	 * 
	 * @param url - the member service URL to check
	 * @return <code>true</code> if URL looks OK
	 */
	protected boolean checkBasicCorrectness(String url, Kernel k){
		String baseURL = k.getContainerProperties().getContainerURL();
		return url!=null && url.startsWith(baseURL);
	}

	protected void reAdd(Kernel kernel, String endpoint) throws Exception
	{
		RegistryHandler handler = kernel.getAttribute(RegistryHandler.class);
		handler.getRegistryClient().refresh(endpoint);
	}

}
