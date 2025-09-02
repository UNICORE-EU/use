package eu.unicore.services.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Model;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.AuthZAttributeStore;
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
	 * this makes sense for the Resource.
	 * <p>
	 * The default implementation does nothing.
	 *
	 * @param securityTokens tokens to be updated. It is never null.
	 */
	public void updateSecurityTokensBeforeAIP(SecurityTokens securityTokens) {}

	/**
	 * set the default owner of this resource, which is either
	 * <ul>
	 *   <li>the effective user, or/li>
	 *   <li>the server, if no client security info is available</li>
	 * </ul>
	 */
	protected void setDefaultOwner(){
		SecurityTokens tokens=getSecurityTokens();
		if(tokens!=null){
			String p = tokens.getEffectiveUserName();
			if (p!=null)
				setOwner(p);
		}
		else{
			setServerAsOwner();
		}
		logger.debug("Owner: {}", ()-> model.getOwnerDN()!=null?
			X500NameUtils.getReadableForm(model.getOwnerDN()): "n/a" );
	}

	protected void setServerAsOwner(){
		String owner = Client.ANONYMOUS_CLIENT_DN;
		X509Credential kernelIdentity = getKernel().getContainerSecurityConfiguration().getCredential();
		if (kernelIdentity != null) {
			owner = kernelIdentity.getSubjectName();
			logger.debug("Setting server as owner of {}/{}", getServiceName(), getUniqueID());
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
	public Client getClient() {
		return AuthZAttributeStore.getClient();
	}

	/**
	 * set the owner
	 * 
	 * @param owner
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
		setOwner(new X500Principal(owner));
	}

	/**
	 * Get the DN of the owner of this resource.
	 * If not set in the security context, this is set to be the server.
	 * If server has no identity (fully insecure mode) it is set to anonymous identity.
	 *
	 * @return owner's DN
	 */
	public String getOwner(){
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
			logger.info("Deleting instances of <{}> for <{}> : {}", 
					serviceName, getClient().getDistinguishedName(), ids);
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

		@Override
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
