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
package de.fzj.unicore.wsrflite.xmlbeans.registry;

import java.util.Calendar;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.impl.InstanceChecker;
import de.fzj.unicore.wsrflite.registry.ServiceRegistryEntryImpl;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Checks and refreshes a registry entry in the local registry.
 * 
 * @author demuth
 * @author schuller
 */
public class RegistryEntryUpdater implements InstanceChecker {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryEntryUpdater.class);
	
	// whether a check for service accessibility should be done
	boolean RUN_EXTERNAL_CHECK = false;

	public boolean check(Home home, String id)throws ResourceUnknownException,PersistenceException{
		Calendar c=home.getTerminationTime(id);
		if(logger.isDebugEnabled()){
			logger.debug("Checking <"+home.getServiceName()+">"+id+" TT = "+(c!=null?c.getTime():"none"));
		}
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
	 *  <li>If the member service is alive, but can't be reached (say, if the gateway is down),
	 * the entry is not refreshed, but stays alive and will be re-checked. 
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
			ServiceRegistryEntryImpl entry=(ServiceRegistryEntryImpl)home.get(id);
			String memberAddress = entry.getModel().getEndpoint();
			
			//check that URL is still basically correct (e.g. hostname, port) 
			if(!checkBasicCorrectness(memberAddress, kernel)){
				logger.debug("Member address "+memberAddress+" is no longer valid, destroying registry entry.");
				entry.destroy();
				home.destroyResource(id);
				//instance is invalid and should be removed from all checks
				return false;
			}
			
			// for WSRF service instances: check if instance is still available in this container
			if (!wsrfResourceExists(kernel, memberAddress))	{
				logger.info("Destroying registry entry for <" + memberAddress+">, because resource does not exist.");
				entry.destroy();
				home.destroyResource(id);
				//instance is invalid and should be removed from all checks
				return false;
			}
			
			if(RUN_EXTERNAL_CHECK){
				if(!runExternalCheck(serviceName, kernel)){
					return true;
				}
			}
			try
			{
				reAdd(home.getKernel(),memberAddress);
				if(logger.isDebugEnabled())logger.debug("Refreshed registry entry for: " +memberAddress);
			}
			catch(Exception e)
			{						
				Log.logException("Error re-adding service entry: ",e,logger);
			}		
		}

		catch(Exception e){
			Log.logException("Could not update WSRF instance of type "+serviceName+" and id: "+id,e,logger);
		}

		// instance is still valid
		return true;
	}

	// makes an external call to make sure the service is reachable (incl. gateway)
	// returns true if the service is accessible
	protected boolean runExternalCheck(String serviceName, Kernel kernel) {
		boolean accessible = true;
		try {
			String uri=WSServerUtilities.makeAddress(serviceName, kernel.getContainerProperties());
			IClientConfiguration auth = kernel.getClientConfiguration();
			HttpClient client=HttpUtils.createClient(uri, auth);
			HttpGet get=new HttpGet(uri+"?wsdl");
			try{
				HttpResponse response=client.execute(get);
				int status=response.getStatusLine().getStatusCode();
				if(status!=HttpServletResponse.SC_OK){
					logger.warn("Error reaching service, not publishing entry to external registry. Error was: "
							+response.getStatusLine().toString());
					accessible = false;
				}
				EntityUtils.consumeQuietly(response.getEntity());
			}
			finally{
				get.reset();
			}
		} catch (Exception e) {
			logger.warn("Could not reach service via web service call. Not publishing entry to external registry.",e);
			accessible = false;
		}
		return accessible;
	}
	
	/**
	 * 
	 * @param url - the member service URL to check
	 * @return <code>true</code> if URL looks OK
	 */
	protected boolean checkBasicCorrectness(String url, Kernel k){
		String baseURL=k.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
		return url!=null && url.startsWith(baseURL);
	}

	protected void reAdd(Kernel kernel, String endpoint) throws Exception
	{
		RegistryHandler handler = kernel.getAttribute(RegistryHandler.class);
		handler.getRegistryClient().refresh(endpoint);
	}

	public static boolean wsrfResourceExists(Kernel kernel, String memberAddress) {
		if(memberAddress.toLowerCase().contains("?res=")) {
			try {
				if(logger.isDebugEnabled()){
					logger.debug("Alive check: "+memberAddress);
				}
				Home serviceHome = kernel.getHome(WSServerUtilities.extractServiceName(memberAddress));
				serviceHome.get(WSServerUtilities.extractResourceID(memberAddress));
			} 
			catch (Exception e) {
				return false;
			}
		}
		return true;
	}
}
