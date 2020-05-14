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
 

package de.fzj.unicore.wsrflite.admin.service;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnavailableException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.Client;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

/**
 * AdminService Home implementation.

 * @author j.daivandy@fz-juelich.de
 */
public class AdminServiceHomeImpl extends WSResourceHomeImpl{
	
	@Override
	protected synchronized AdminServiceImpl doCreateInstance() {
		return new AdminServiceImpl(getKernel(), this);
	}

	private AdminServiceImpl getInstance(){
		AdminServiceImpl i=new AdminServiceImpl(getKernel(), this);
		i.setHome(this);
		i.setKernel(getKernel());
		return i;
	}
	
	@Override
	public synchronized Resource get(String id) throws ResourceUnknownException,
			PersistenceException {
		return getInstance();
	}

	@Override
	public synchronized Resource getForUpdate(String id) throws ResourceUnknownException,
			ResourceUnavailableException {
		return getInstance();
	}	
	
	/**
	 * admin service instances are always owned by the server
	 */
	@Override
	public String getOwner(String instanceID){
		String owner = Client.ANONYMOUS_CLIENT_DN;
		X509Credential kernelIdentity = getKernel().getContainerSecurityConfiguration().getCredential();
		if (kernelIdentity != null) {
			owner = kernelIdentity.getSubjectName();
		}
		return owner;
	}
}
