/**
 * Copyright (c) 2012 Forschungszentrum Juelich GmbH 
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

package de.fzj.unicore.wsrflite.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Model;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.SecurityTokens;
import eu.unicore.util.Log;

/**
 * Implements the security aspects of {@link Resource}. 
 * 
 * This class holds:
 * <ul>
 * <li>the DN of the service's owner</li>
 * <li>the information about VOs that this resource is shared with</li>
 * </ul>
 * 
 * @author schuller
 * @author golbi
 */
public abstract class SecuredResourceImpl implements Resource {

	private static final Logger logger=Log.getLogger(Log.UNICORE, SecuredResourceImpl.class);
	
	public static enum AccessLevel {
		NORMAL, OWNER;
	}
	
	/**
	 * used for passing an initial ACL (a List of ACLEntry objects) to a service instance
	 */
	public static final String INITPARAM_INITIAL_ACL=SecuredResourceImpl.class.getName()+".init.initialACL";

	
	protected SecuredResourceModel model;

	@Override
	public SecuredResourceModel getModel(){
		return model;
	}
	
	@Override
	public void setModel(Model model){
		this.model=(SecuredResourceModel)model;
	}
	
	@Override
	public String getUniqueID(){
		return model.getUniqueID();
	}

	@Override
	public void initialise(InitParameters initParams) throws Exception {
		if(model==null){
			model=new SecuredResourceModel();
		}
		model.getAcl().addAll(initParams.acl);
		if(initParams.ownerDN!=null){
			setOwner(initParams.ownerDN);
		}
		else{
			setDefaultOwner();
		}
	}

	/**
	 * Allows the service to perform object specific updates on the security tokens
	 * <b>before</b> those will be used for attributes retrieval and subsequently 
	 * for authorization. At the moment of this method invocation it is guaranteed
	 * that authentication step was performed (including trust delegation checking etc)
	 * and that user preferences were processed. 
	 * <p>
	 * Example use case of this method is to provide persisted user preferences if
	 * this makes sense for the WS-Resource.
	 * <p>
	 * The default implementation does nothing.
	 *   
	 * @param securityTokens tokens to be updated. It is never null.
	 */
	public void updateSecurityTokensBeforeAIP(SecurityTokens securityTokens) {
	}


	/**
	 * set the default owner of this resource, which is 
	 * <ul>
	 *   <li>the original user if trust delegation info is present, or we have the user cert</li>
	 *   <li>the consignor, if available</li>
	 *   <li>the server, if no client security info is available</li>
	 * </ul>
	 */
	protected void setDefaultOwner(){
		SecurityTokens tokens=getSecurityTokens();
		if(tokens!=null){
			String p=tokens.getEffectiveUserName();
			if (p!=null)
				setOwner(p);
		}
		else{
			setServerAsOwner();
		}
		if(logger.isDebugEnabled()){
			String ownerDN=model.getOwnerDN();
			if (ownerDN!=null) {
				logger.debug("Owner: "+X500NameUtils.getReadableForm(ownerDN));
			} else {
				logger.debug("Owner could not be assigned.");
			}
		}
	}

	protected void setServerAsOwner(){
		String owner = Client.ANONYMOUS_CLIENT_DN;
		// set server as owner
		X509Credential kernelIdentity = getKernel().getContainerSecurityConfiguration().getCredential();
		if (kernelIdentity != null) {
			owner = kernelIdentity.getSubjectName();
			if(logger.isDebugEnabled()){
				logger.debug("Setting server as owner of "+getServiceName()+"<"+getUniqueID()+">");
			}
		}
		setOwner(owner);
	}
	/**
	 * get the security tokens that were extracted from the request.
	 * This method is here for backwards compatibility. It is suggested to use 
	 * {@link AuthZAttributeStore#getTokens()} instead
	 */
	public SecurityTokens getSecurityTokens() {
		return AuthZAttributeStore.getTokens();
	}

	/**
	 * Get the Client object. 
	 * This method is here for backwards compatibility. It is suggested to use 
	 * {@link AuthZAttributeStore#getClient()} instead
	 */
	public synchronized Client getClient() {
		return AuthZAttributeStore.getClient();
	}

	/**
	 * set the owner using an {@link X500Principal}
	 * 
	 * @param owner - an {@link X500Principal}
	 */
	public void setOwner(X500Principal owner){
		model.setOwnerDN(owner.getName());
	}

	/**
	 * set the owner using the String form of an {@link X500Principal}
	 * 
	 * @param owner
	 */
	public void setOwner(String owner){
		model.setOwnerDN(owner);
	}

	/**
	 * Get the DN of the owner of this resource.
	 * If not set in the security context, this is set to be the server.
	 * If server has no identity (fully insecure mode) it is set to anonymous identity.
	 *
	 * @return owner's DN
	 */
	public synchronized String getOwner(){
		String ownerDn = model.getOwnerDN();
		return ownerDn!=null ? ownerDn : Client.ANONYMOUS_CLIENT_DN;
	}
		
	public boolean isOwnerLevelAccess() {
		Client c = getClient();
		if(!getKernel().getContainerSecurityConfiguration().isAccessControlEnabled())return true;
		boolean owner = X500NameUtils.equal(c.getDistinguishedName(),model.getOwnerDN());
		boolean admin = "admin".equals(c.getRole().getName());
		boolean server = getKernel().getSecurityManager().isServer(c);
		return owner || admin || server;
	}
	

	/**
	 * Delete the given children. Deletion includes removal from the 
	 * model AND actual removal (destruction) of the resource
	 *  
	 * @param children
	 */
	public Collection<String> deleteChildren(Collection<String>children) {
		Map<String,Collection<String>>toRemove = new HashMap<String,Collection<String>>();
		Map<String,List<String>>myChildren = getModel().getChildren();
		for(String serviceName: myChildren.keySet()){
			for(String id: myChildren.get(serviceName)){
				if(children.contains(id)){
					Collection<String>ids = toRemove.get(serviceName);
					if(ids == null){
						ids = new HashSet<String>();
						toRemove.put(serviceName, ids);
					}
					ids.add(id);
				}
			}
		}
		for(String serviceName: toRemove.keySet()){
			final Home h = getHome();
			final Collection<String>ids = toRemove.get(serviceName);
			final Client client = getClient();
			Runnable r = new AsyncChildDelete(client, h, ids);
			logger.info("Deleting instances of <"+serviceName+
					"> for <"+getClient().getDistinguishedName()+"> : "+ids);
			getKernel().getContainerProperties().getThreadingServices().getExecutorService().execute(r);
		}
		return getModel().removeChildren(children);
	}



	public static class AsyncChildDelete implements Runnable{

		private final Collection<String>toRemove;

		private final Home home;
		
		private final Client client;
		
		public AsyncChildDelete(Client client, Home home, Collection<String>toRemove){
			this.toRemove = toRemove;
			this.home = home;
			this.client = client;
		}
		
		public void run(){
			for(String j: toRemove){
				try{
					Resource r = home.getForUpdate(j);
					if(r instanceof SecuredResourceImpl){
						ActionDescriptor action =  new ActionDescriptor("Destroy", OperationType.modify);
						r.getKernel().getSecurityManager().checkAuthorisation(client,action,(SecuredResourceImpl)r);
					}
					r.destroy();
					home.destroyResource(j);
				}catch(ResourceUnknownException r){
					// ignore
				}catch(AuthorisationException ae){
					// ignore
				}catch(Exception ex){
					Log.logException("Could not destroy "+home.getServiceName()+" "+j, ex, logger);
				}
			}
		}
	}
}
