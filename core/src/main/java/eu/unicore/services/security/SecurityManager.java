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

package eu.unicore.services.security;

import java.lang.reflect.Constructor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.Queue;
import eu.unicore.security.Role;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.Xlogin;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.SecuredResourceImpl;
import eu.unicore.services.impl.SecuredResourceModel;
import eu.unicore.services.security.pdp.AcceptingPdp;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.AttributeHandlingCallback;
import eu.unicore.services.security.util.AttributeSourcesChain;
import eu.unicore.services.security.util.BaseAttributeSourcesChain.CombiningPolicy;
import eu.unicore.services.security.util.DynamicAttributeSourcesChain;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Provides an entry point for security related functionality of USE. In particular:
 * <ul>
 * <li> setup of Client object including attributes retrieval from AIP
 * <li> allow to use PDP to check resource access
 * <li> allow to check signatures according to the local policy 
 * </ul> 
 * 
 * @author schuller
 * @author golbi
 */
public final class SecurityManager {

	private static final Logger logger=Log.getLogger(Log.SECURITY,SecurityManager.class);
	
	public static final String UNKNOWN_ACTION = "___ANY_ACTION___";

	
	private static final ThreadLocal<Boolean> localCalls = new ThreadLocal<>();
	
	private final Set<AttributeHandlingCallback> attribHandlingCallbacks=new HashSet<>();
	
	private final OperationTypesUtil operationTypesUtil;

	private final Kernel kernel;

	private ContainerSecurityProperties securityConfig;
	private IAttributeSource aip;
	private IDynamicAttributeSource dap;
	private UnicoreXPDP pdp;
	
	public SecurityManager(Kernel kernel) {
		this.kernel = kernel;
		this.securityConfig = kernel.getContainerSecurityConfiguration();
		this.operationTypesUtil = new OperationTypesUtil();
	}

	public void start() {
		this.aip = createAttributeSource(kernel);
		if(aip instanceof ISubSystem)kernel.register((ISubSystem)aip);
		this.dap = createDynamicAttributeSource(kernel);
		if(dap instanceof ISubSystem)kernel.register((ISubSystem)dap);
		this.pdp = createPDP(kernel);
		if(pdp instanceof ISubSystem)kernel.register((ISubSystem)pdp);
	}

	public IAttributeSource getAip() {
		return aip;
	}

	public IDynamicAttributeSource getDap() {
		return dap;
	}

	public UnicoreXPDP getPdp() {
		return pdp;
	}

	/**
	 * add a callback class for dealing with additional security attributes 
	 * 
	 * @param aac - an {@link AttributeHandlingCallback}
	 */
	public void addCallback(AttributeHandlingCallback aac){
		attribHandlingCallbacks.add(aac);
	}

	/**
	 * Returns an attribute map for a set of security tokens from the configured Attribute Source
	 * @param tokens
	 * @return attributes
	 */
	public SubjectAttributesHolder establishAttributes(final SecurityTokens tokens) 
			throws Exception {
		//get the list of user preferences from the User assertion
		Map<String, String[]> preferences = tokens.getUserPreferences();
		String preferedVos[] = null;
		if (preferences != null && preferences.get(IAttributeSource.ATTRIBUTE_SELECTED_VO) != null) {
			preferedVos = new String [] {preferences.get(IAttributeSource.ATTRIBUTE_SELECTED_VO)[0]};
		} else {
			preferedVos = securityConfig.getDefaultVOs();
		}
		SubjectAttributesHolder initial = new SubjectAttributesHolder(preferedVos);
		return aip.getAttributes(tokens, initial);
	}

	/**
	 * Sets up the Xlogin object in the Client.
	 * @param client
	 * @param preferences
	 * @param validAttributes
	 * @param defaultAttributes
	 */
	private static void handleXlogin(Client client, Map<String, String[]> preferences, 
			Map<String, String[]> validAttributes, Map<String, String[]> defaultAttributes) {
		String[] validXlogins = validAttributes.get(IAttributeSource.ATTRIBUTE_XLOGIN);
		String[] defaultXlogin = defaultAttributes.get(IAttributeSource.ATTRIBUTE_XLOGIN);
		
		String[] validPGroups = validAttributes.get(IAttributeSource.ATTRIBUTE_GROUP);
		if (validPGroups == null)
			validPGroups = new String[0];
		String[] defaultPGroup = defaultAttributes.get(IAttributeSource.ATTRIBUTE_GROUP);
		if (defaultPGroup == null)
			defaultPGroup = new String[0];
		
		String[] validSupGroups = validAttributes.get(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS);
		if (validSupGroups == null)
			validSupGroups = new String[0];
		String[] defaultSupGroups = defaultAttributes.get(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS);
		if (defaultSupGroups == null)
			defaultSupGroups = new String[0];
		
		Set<String> validGroups = new HashSet<>();
		Collections.addAll(validGroups, validPGroups);
		Collections.addAll(validGroups, validSupGroups);
		String[] pAddDefaultGids = defaultAttributes.get(IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS);		
		if (pAddDefaultGids == null || pAddDefaultGids.length == 0)
			pAddDefaultGids = validAttributes.get(IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS);
		
		//uid must be always set. XLogin object wraps uid, gid and supp. gids.
		if (validXlogins!=null && validXlogins.length > 0){
			//create XLogin with valid values
			Xlogin xlogin = new Xlogin(validXlogins, 
				validGroups.toArray(new String[validGroups.size()]));
			
			//set defaults from AS
			if (defaultXlogin != null && defaultXlogin.length > 0) {
				xlogin.setSelectedLogin(defaultXlogin[0]);
			}
			if (defaultPGroup.length > 0)
				xlogin.setSelectedGroup(defaultPGroup[0]);
			if (defaultSupGroups.length > 0)
				xlogin.setSelectedSupplementaryGroups(defaultSupGroups);
			
			//handle user preferences
			String[] reqXlogin = preferences.get(IAttributeSource.ATTRIBUTE_XLOGIN);
			if (reqXlogin!=null && reqXlogin.length > 0)
				xlogin.setSelectedLogin(reqXlogin[0]);
			
			String[] reqGroup = preferences.get(IAttributeSource.ATTRIBUTE_GROUP);
			if (reqGroup!=null && reqGroup.length > 0)
				xlogin.setSelectedGroup(reqGroup[0]);
			
			String[] reqSupGroups = preferences.get(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS);
			if (reqSupGroups != null && reqSupGroups.length > 0)
				xlogin.setSelectedSupplementaryGroups(reqSupGroups);
			
			//This is special - we always allow for changing this by the user.
			String[] reqAddDefaultGroups = preferences.get(IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS);
			if (reqAddDefaultGroups != null && reqAddDefaultGroups.length > 0) {
				if (reqAddDefaultGroups[0].equalsIgnoreCase("true"))
					xlogin.setAddDefaultGroups(true);
				else if (reqAddDefaultGroups[0].equalsIgnoreCase("false"))
					xlogin.setAddDefaultGroups(false);
				else
					throw new SecurityException("Requested value <"+reqAddDefaultGroups[0]+
						"> is invalid for " + IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS +
						" attribute; use 'true' or 'false'.");
			} else if (pAddDefaultGids != null && pAddDefaultGids.length > 0) {
				if (pAddDefaultGids[0].equalsIgnoreCase("true"))
					xlogin.setAddDefaultGroups(true);
				else if (pAddDefaultGids[0].equalsIgnoreCase("false"))
					xlogin.setAddDefaultGroups(false);
			}
			
			client.setXlogin(xlogin);
		}
	}
	
	/**
	 * Sets up the role object in the Client.
	 * @param client
	 * @param preferences
	 * @param validAttributes
	 * @param defaultAttributes
	 */
	private static void handleRole(Client client, Map<String, String[]> preferences, 
			Map<String, String[]> validAttributes, Map<String, String[]> defaultAttributes) {
		Role r;
		String[] validRoles = validAttributes.get(IAttributeSource.ATTRIBUTE_ROLE);
		if (validRoles == null || validRoles.length == 0) {
			r = new Role(Role.ROLE_ANONYMOUS, "default role");
		} else {
			r = new Role(validRoles);
			String[] defaultRole = defaultAttributes.get(IAttributeSource.ATTRIBUTE_ROLE);
			String[] prefRole = preferences.get(IAttributeSource.ATTRIBUTE_ROLE);
			if (prefRole != null && prefRole.length > 0) {
				if (r.isValid(prefRole[0])) {
					r.setName(prefRole[0]);
					r.setDescription("user's preferred role");
				} else
					throw new SecurityException("Requested role <"+prefRole[0]+"> is not available.");
			} else if (defaultRole != null && defaultRole.length > 0) {
				r.setName(defaultRole[0]);
				r.setDescription("role from attribute source");
			} else {
				//we left name unset - Role will select the first valid
				r.setDescription("default role from attribute source");
			}
		}
		
		client.setRole(r);
	}

	/**
	 * Sets up the queue object in the Client. Only small part of the job is done here
	 * as available queues are further refined by IDB settings and only then user 
	 * preferences are checked 
	 * @param client
	 * @param preferences
	 * @param validAttributes
	 * @param defaultAttributes
	 */
	private static void handleQueue(Client client, Map<String, String[]> preferences, 
			Map<String, String[]> validAttributes, Map<String, String[]> defaultAttributes) {
		Queue q = new Queue();
		String[] validQueues = validAttributes.get(IAttributeSource.ATTRIBUTE_QUEUES);
		String[] defQueue = defaultAttributes.get(IAttributeSource.ATTRIBUTE_QUEUES);
		if (validQueues != null && validQueues.length > 0)
		{
			q.setValidQueues(validQueues);
			if (defQueue != null && defQueue.length > 0)
				q.setSelectedQueue(defQueue[0]);
		}
		client.setQueue(q);
	}

	private static void handleVo(String selectedVo, Client client, Map<String, String[]> validAttributes) {
		String[] vos = validAttributes.get(IAttributeSource.ATTRIBUTE_VOS);
		if (vos != null)
			client.setVos(vos);
		
		//this check should not be needed but for double security...
		if (selectedVo != null) {
			if (vos == null) {
				logger.fatal("BUG! attribute handlers set a VO for the request, but the user is not member of any VO");
				throw new SecurityException("BUG! attribute handlers set a VO for the request, but the user is not member of any VO");
			}
			int i;
			for (i=0; i<vos.length; i++)
				if (vos[i].equals(selectedVo))
					break;
			if (i == vos.length) {
				logger.fatal("BUG! attribute handlers set a VO for the request, but the user is not a member of this VO");
				throw new SecurityException("BUG! attribute handlers set a VO for the request, but the user is not a member of this VO");
			}
			
			client.setVo(selectedVo);
		}
	}
	
	
	/**
	 * Sets up static authorisation attributes: VOs and role.
	 * @param client
	 * @param tokens
	 */
	private void assembleClientAttributes(Client client, SecurityTokens tokens) {
		if (isServer(client)) {
			Role r=new Role("server", "Server self access pseudo-role");
			client.setRole(r);
		} else {
			//setup client with authorisation attributes
			SubjectAttributesHolder subAttributes;
			try {
				subAttributes = establishAttributes(tokens);
			} catch (Exception e) {
				throw new SecurityException("Exception when getting " +
						"attributes for the client.", e);
			}
			
			client.setSubjectAttributes(subAttributes);
			//get the list of user preferences from the User assertion
			
			Map<String, String[]> preferences = tokens.getUserPreferences();
			
			Map<String, String[]> validAttributes = client.getSubjectAttributes().getValidIncarnationAttributes();
			Map<String, String[]> defaultAttributes = client.getSubjectAttributes().getIncarnationAttributes();
			
			handleRole(client, preferences, validAttributes, defaultAttributes);
			handleVo(subAttributes.getSelectedVo(), client, validAttributes);
			
			//handle additional attributes
			for(AttributeHandlingCallback a: attribHandlingCallbacks){
				Map<String, String> attribs=a.extractAttributes(tokens);
				if(attribs!=null){
					client.getExtraAttributes().putAll(attribs);
				}
			}
		}
	}

	/**
	 * Typical client setup is done here (when security is ON and we don't handle a local call).
	 * This method setups the Client object with credentials previously verified and resolve user's attributes.
	 * As this last step might require a network calls this method is a heavy one.
	 *
	 * @param tokens - Security tokens
	 * @return fully initialized Client object 
	 */
	private Client createSecureClient(final SecurityTokens tokens) {
		Client client=new Client();
		client.setAuthenticatedClient(tokens);
		if (client.getType() != Client.Type.ANONYMOUS) {
			assembleClientAttributes(client, tokens);
			if(logger.isDebugEnabled()){
				logger.debug("Client info (after static AIPs):\n{}", client.toString());
			}
		}
		return client;
	}
	
	/**
	 * Create an Client object. This will use the supplied
	 * security tokens to make a call to an attribute information point (such as the XUUDB)
	 * and set client attributes such as role, xlogin, etc based on the
	 * AIP's reply.
	 * 
	 * @param tokens - Security tokens
	 * @return authorised Client object 
	 */
	public Client createClientWithAttributes(final SecurityTokens tokens) {
		Client client;
		
		// for local call, and for non local calls when the security is enabled
		if (isLocalCall()) {
			client = new Client();
			client.setLocalClient();
		} else if (securityConfig.isAccessControlEnabled()) {
			client = createSecureClient(tokens);
		} else {
			//in all other cases client should be left as anonymous.
			client = new Client();
		}
		
		if (isTrustedAgent(client)) {
			if(logger.isDebugEnabled()) {
				String consignor = tokens.getConsignorName(); 
				logger.debug("Accept trusted-agent {} to work for selected user {}",
						X500NameUtils.getReadableForm(consignor),
						X500NameUtils.getReadableForm(tokens.getUserName()));
			}
			tokens.setConsignorTrusted(true);
		}
		//now we know the client name we can put it into the log context
		ThreadContext.put("clientName", client.getDistinguishedName());
		return client;
	}

	private Decision checkAuthzInternal(Client c, ActionDescriptor action, ResourceDescriptor d) {
		PDPResult res;
		try {
			res = pdp.checkAuthorisation(c, action, d);
		} catch(Exception e) {
			throw new AuthorisationException("Access denied due to PDP error: " + e, e);
		}

		if (res.getDecision().equals(PDPResult.Decision.UNCLEAR)) {
			logger.warn("The UNICORE/X PDP was unable to make a definitive decision, " +
					"check your policy files and consult other log messages. "
					+ "The PDP returned the following status message: "+res.getMessage());
		}
		if (res.getDecision().equals(PDPResult.Decision.DENY) && logger.isDebugEnabled()) {
			if (res.getMessage() != null && res.getMessage().length() > 0)
				logger.debug("The UNICORE/X PDP denied the request: {}", res.getMessage());
			else
				logger.debug("The UNICORE/X PDP denied the request");
		}
		return res.getDecision();
	}
	
	/**
	 * Check access by evaluating the XACML policies. 
	 * If access is DENIED or UNCLEAR then {@link AuthorisationException} is thrown. 
	 *  
	 * @param c
	 * @param action
	 * @param d
	 * @throws AuthorisationException
	 */
	public void checkAuthorisation(Client c, ActionDescriptor action, ResourceDescriptor d) 
			throws AuthorisationException {
		Decision decision = checkAuthzInternal(c, action, d);
		if (!decision.equals(PDPResult.Decision.PERMIT)){
			String msg="Access denied for "+c.getDistinguishedName()+" on resource "+d;
			logger.debug(msg);
			throw new AuthorisationException(msg);
		}
	}

	/**
	 * convenience method that checks user ACL and access via PDP
	 * 
	 * @param client - the requesting client
	 * @param action - the action
	 * @param resource - the resource
	 * @throws AuthorisationException - in case of access denied
	 */
	public void checkAuthorisation(Client client, ActionDescriptor action, Resource resource) 
			throws AuthorisationException {
		ResourceDescriptor rd = checkACL(client, action.getActionType(), resource);
		checkAuthorisation(client, action, rd);
	}

	/**
	 * check ACL and create a resource descriptor
	 * @param client
	 * @param actionType
	 * @param resource
	 * @return resource descriptor suitable for further PDP access control
	 */
	public ResourceDescriptor checkACL( Client client, OperationType actionType,Resource resource){
		String serviceName = resource.getServiceName();
		String owner = null;
		boolean aclCheckPassed = false;
		if(resource instanceof SecuredResourceImpl){
			SecuredResourceModel secureModel = ((SecuredResourceImpl)resource).getModel();
			owner = secureModel.getOwnerDN();
			// user ACL
			aclCheckPassed = checkAcl(secureModel.getAcl(),actionType,client); 
		}
		ResourceDescriptor rd = new ResourceDescriptor(serviceName, resource.getUniqueID(), owner);
		rd.setAclCheckOK(aclCheckPassed);
		return rd;
	}
	
	/**
	 * Provides an approximate answer to the question whether the given 
	 * can client access the given (server-local) endpoint?
	 * This will not give the correct result if the action plays a role.
	 * Therefore note that result is only approximate! With high probability the
	 * client will be able to use the wsResource, but it might happen that some 
	 * actions are explicitly banned by the policy.
	 * <b>
	 * Never use this method for performing an actual access authorization!   
	 * </b>
	 * 
	 * @param client - the client
	 * @param serviceName - can be <code>null</code>
	 * @param wsResourceID - can be <code>null</code>
	 * @param owner - the owner DN
	 * @param acl - ACL entries for the resource
	 * @return true if the resource/endpoint can be accessed
	 */
	public boolean isAccessible(Client client, String serviceName, String wsResourceID, String owner, List<ACLEntry> acl) throws Exception{
		if(!securityConfig.isAccessControlEnabled())
			return true;
		if(isServer(client))
			return true;
		ResourceDescriptor resource=new ResourceDescriptor(serviceName, wsResourceID, owner);
		boolean aclCheckPassed = checkAcl(acl, OperationType.read, client);
		resource.setAclCheckOK(aclCheckPassed);
		Decision decision = checkAuthzInternal(client, new ActionDescriptor(UNKNOWN_ACTION, 
				OperationType.read), resource);
		
		if (!decision.equals(PDPResult.Decision.PERMIT))
			return false;
		else
			return true;
	}
	
	/**
	 * Should be invoked only for previously authorised clients, to collect their dynamic, incarnation related
	 * attributes.
	 */
	public void collectDynamicAttributes(Client client) {
		if (isServer(client))
			return;
		SecurityTokens tokens = client.getSecurityTokens();
		
		//setup client with authorisation attributes
		SubjectAttributesHolder dynamicSubAttributes;
		SubjectAttributesHolder staticSubAttributes = client.getSubjectAttributes();
		if (!(dap instanceof NullAttributeSource))
		{		
			try {
				dynamicSubAttributes = dap.getAttributes(client, staticSubAttributes);
			} catch (Exception e) {
				throw new SecurityException("Exception when getting dynamic attributes for the client.", e);
			}
			logger.debug("Client's dynamic attributes: {}", dynamicSubAttributes);
			CombiningPolicy combiner = DynamicAttributeSourcesChain.MERGE_LAST_OVERRIDES;
			combiner.combineAttributes(staticSubAttributes, dynamicSubAttributes);
		}
		//get the list of user preferences from the User assertion
		Map<String, String[]> preferences = tokens.getUserPreferences(); 
		
		Map<String, String[]> validAttributes = client.getSubjectAttributes().getValidIncarnationAttributes();
		Map<String, String[]> defaultAttributes = client.getSubjectAttributes().getIncarnationAttributes();
		
		handleXlogin(client, preferences, validAttributes, defaultAttributes);
		handleQueue(client, preferences, validAttributes, defaultAttributes);
		
		logger.debug("Client info (final):\n{}", ()-> client.toString());
	}
	
	/**
	 * check that the given ACL allows the requested operation
	 * 
	 * @param acl - list of acl entries (can be null)
	 * @param opType - operation type (read, modify, ...)
	 * @param client
	 * @return <code>true</code> if there is an access control entry allowing access
	 */
	public boolean checkAcl(List<ACLEntry> acl, OperationType opType, Client client){
		if(acl==null)return false;
		for(ACLEntry e: acl){
			if(e.allowed(opType, client)){
				logger.debug("Access granted by ACL entry {}", e);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * for the current thread, set the "local call" flag. This should be used always in 
	 * using a try-finally construct, i.e.
	 * 
	 * <pre>
	 *  SecurityManager.setLocalCall();
	 *  try{
	 *    //... perform call
	 *  }
	 *  finally{
	 *    SecurityManager.clearLocalCall();
	 *  }
	 * </pre> 
	 */
	public static void setLocalCall(){
		localCalls.set(Boolean.TRUE);
	}
	
	/**
	 * for the current thread, clear the "local call" flag
	 */
	public static void clearLocalCall(){
		localCalls.set(null);
	}
	
	/**
	 * check whether the current request is local (i.e. made from within the same VM)
	 */
	public static boolean isLocalCall(){
		return Boolean.TRUE.equals(localCalls.get());
	}
	
	/**
	 * helper method to get the server cert from the Kernel security config
	 * @return X509Certificate or <code>null</code> if not available
	 */
	public X509Certificate getServerCert() {
		if (securityConfig.getCredential() != null)
			return securityConfig.getCredential().getCertificate();
		return null;
	}
	
	/**
	 * helper method to get the server DN (RFC2253) from the Kernel security config
	 * @return X509Certificate or <code>null</code> if not available
	 */
	public String getServerIdentity() {
		if (getServerCert() != null)
			return getServerCert().getSubjectX500Principal().getName();
		return null;
	}
	
	/**
	 * checks whether the given client has the server identity
	 */
	public boolean isServer(Client c){
		if (c==null)
			throw new IllegalArgumentException("client can not be null");
		X509Certificate serverCert = getServerCert();
		if (serverCert == null)
			return false;

		String dn = serverCert.getSubjectX500Principal().getName();
		if(logger.isTraceEnabled()){
			logger.trace("Check server=<{}> vs client=<{}>",
					X500NameUtils.getReadableForm(serverCert.getSubjectX500Principal()),
					X500NameUtils.getReadableForm(c.getDistinguishedName()));
		}
		return X500NameUtils.equal(dn, c.getDistinguishedName());
	}

	/**
	 * checks whether the given DN is the server identity
	 */
	public boolean isServer(String callerDn){
		X509Certificate serverCert = getServerCert();
		return serverCert == null ? false : X500NameUtils.equal(
				serverCert.getSubjectX500Principal(), callerDn);
	}

	/**
	 * checks whether the given client has the "trusted agent" role
	 */
	private static boolean isTrustedAgent(Client c){
		try{
			return IAttributeSource.ROLE_TRUSTED_AGENT.equals(c.getRole().getName());
		}catch(Exception e){}
		return false;
	}
	
	public OperationTypesUtil getOperationTypesUtil() {
		return operationTypesUtil;
	}	
	
	public void setConfiguration(ContainerSecurityProperties sp) {
		this.securityConfig = sp;
	}
	
	private static IAttributeSource createAttributeSource(Kernel kernel) 
			throws ConfigurationException {
		ContainerSecurityProperties sp = kernel.getContainerSecurityConfiguration();
		String order = sp.getAIPOrder(); 
		if (order == null) {
			logger.info("No attribute source is defined in the configuration, " +
					"users won't have any authorisation attributes assigned");
			return new NullAttributeSource();
		}
		return new AttributeSourcesChain(kernel);
	}
	
	private static IDynamicAttributeSource createDynamicAttributeSource(Kernel kernel) 
			throws ConfigurationException {
		ContainerSecurityProperties sp = kernel.getContainerSecurityConfiguration();
		String order = sp.getDAPOrder(); 
		if (order == null) {
			logger.info("No dynamic attribute source is defined in the configuration, " +
					"users won't have any dynamic incarnation attributes assigned");
			return new NullAttributeSource();
		}
		return new DynamicAttributeSourcesChain(kernel);
	}

	@SuppressWarnings("unchecked")
	private static UnicoreXPDP createPDP(Kernel kernel) {
		ContainerSecurityProperties secProps = kernel.getContainerSecurityConfiguration();
		if (!secProps.isAccessControlEnabled()) {
			return new AcceptingPdp();
		}
		Class<? extends UnicoreXPDP> pdpClazz = secProps.getPDPClass();
		//this is as use-pdp is not available at compile time and still we want to provide a sensible default for admins.
		if (pdpClazz == null) {
			try {
				pdpClazz = (Class<? extends UnicoreXPDP>) Class.forName("eu.unicore.uas.pdp.local.LocalHerasafPDP");
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException("The default eu.unicore.uas.pdp.local.LocalHerasafPDP PDP is not available and PDP was not configured.");
			}
		}
		try {
			Constructor<? extends UnicoreXPDP> constructor = pdpClazz.getConstructor();
			logger.info("Using PDP class <{}>", pdpClazz.getName());
			UnicoreXPDP pdp = constructor.newInstance();
			pdp.setKernel(kernel);
			return pdp;
		}catch(Exception e) {
			throw new ConfigurationException("Can't create a PDP.", e);
		}
	}
}
