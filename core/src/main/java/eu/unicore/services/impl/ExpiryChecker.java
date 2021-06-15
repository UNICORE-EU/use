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


package eu.unicore.services.impl;

import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.persistence.Store;
import eu.unicore.util.Log;

/**
 * Checks Resource instance for lifetime expiry.
 * 
 * @author schuller
 */
public class ExpiryChecker implements InstanceChecker {

	private static final Logger logger=Log.getLogger(Log.UNICORE,ExpiryChecker.class);

	public boolean check(Home home, String id)throws ResourceUnknownException, PersistenceException {
		Calendar c=home.getTerminationTime(id);
		if(c==null)return false;
		if(logger.isDebugEnabled()){
			logger.debug("Checking <"+home.getServiceName()+">"+id+" TT = "+c.getTime());
		}
		boolean comp=c.compareTo(Calendar.getInstance())<=0;
		return comp;
	}

	public boolean process(Home home, String id) {
		String serviceName = home.getServiceName();
		Resource i;
		try{
			logger.debug("Destroying "+serviceName+"<"+id+">");
			i=home.getForUpdate(id);
			try{
				i.destroy();
			}
			catch(Exception ex){
				Log.logException("Could not perform cleanup for "+serviceName+"<"+id+">",ex,logger);
			}
			Store s=home.getStore();
			s.unlock(i);
		}catch(ResourceUnknownException rue){
			Log.logException("Could not find instance "+serviceName+"<"+id+">",rue,logger);
			// instance is invalid and should be removed from all checks
		}catch(ResourceUnavailableException e){
			Log.logException("Could not lock instance "+serviceName+"<"+id+">",e,logger);
			return true; //need to try again
		}
		try{
			((DefaultHome)home).removeFromStorage(id);
		}catch(Exception e){
			Log.logException("Error cleaning up "+serviceName+"<"+id+">",e,logger);
		}
		return false;
	}


}
