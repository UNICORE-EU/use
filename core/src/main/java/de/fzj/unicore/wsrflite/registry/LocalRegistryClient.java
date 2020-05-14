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

package de.fzj.unicore.wsrflite.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;

/**
 * For accessing the local registry, publishing entries
 * 
 * @author schuller
 */
public class LocalRegistryClient{

	private final String resID;

	private final String serviceName;

	private final Kernel kernel;

	public LocalRegistryClient(String serviceName, String resID, Kernel kernel)throws Exception {
		this.kernel=kernel;
		this.serviceName = serviceName;
		this.resID = resID;
	} 

	public LocalRegistryClient(Kernel kernel)throws Exception {
		this("Registry", "default_registry", kernel);
	} 

	public String addEntry(String endpoint, Map<String,String>content, Calendar requestedTT) 
	throws Exception {
		Home home = getHome();
		ServiceRegistryImpl reg = null;
		try{
			reg = (ServiceRegistryImpl)home.getForUpdate(resID);
			return reg.addEntry(endpoint, content, requestedTT);
		}finally{
			if(reg!=null)home.persist(reg);
		}
	}
	
	public void refresh(String endpoint) throws Exception {
		Home home = getHome();
		ServiceRegistryImpl reg = null;
		try{
			reg = (ServiceRegistryImpl)home.getForUpdate(resID);
			reg.refresh(endpoint);
		}finally{
			if(reg!=null)home.persist(reg);
		}
	}
	
	public Collection<Map<String,String>> listEntries() throws Exception {
		Collection<Map<String,String>> res = new ArrayList<>();
		ServiceRegistryImpl reg = null;
		if(kernel.getMessaging().hasMessages(resID)){
			reg = (ServiceRegistryImpl)getHome().refresh(resID);
		}
		else{
			reg = get();
		}
		res.addAll(reg.getModel().getContents().values());
		return res;
	}

	private ServiceRegistryImpl get() throws PersistenceException {
		return (ServiceRegistryImpl)(getHome().get(resID));
	}
	
	private Home getHome(){
		Home home = kernel.getHome(serviceName);
		if(home==null)throw new IllegalStateException("No registry service deployed");
		return home;
	}
}
