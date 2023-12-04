/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 22, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.saml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.uas.security.saml.conf.IPullConfiguration;
import eu.unicore.uas.security.saml.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Pull user attributes via SAML
 *  
 * @author K. Benedyczak
 */
public class SAMLAttributeSource implements IAttributeSource, ExternalSystemConnector
{
	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, SAMLAttributeSource.class);

	protected PropertiesBasedConfiguration conf;
	protected UnicoreAttributesHandler specialAttrsHandler;
	protected String configFile;
	protected String name;
	protected boolean isEnabled = false;
	protected Kernel kernel;
	
	private SAMLAttributeFetcher fetcher;

	protected Status status = Status.UNKNOWN;	

	protected String statusMessage = "N/A";

	protected CircuitBreaker cb;

	@Override
	public void configure(String name) throws ConfigurationException {
		initConfig(log, name);
	}

	@Override
	public void start(Kernel kernel) throws Exception {
		this.kernel=kernel;

		cb = new CircuitBreaker("Attribute_Source_"+name);
		kernel.getMetricRegistry().register(cb.getName(), cb);
		
		try
		{
			IClientConfiguration cc = kernel.getClientConfiguration();
			if(cc instanceof DefaultClientConfiguration &&
				conf.getValue(PropertiesBasedConfiguration.CFG_SERVER_USERNAME)!=null){
					DefaultClientConfiguration dcc = (DefaultClientConfiguration)cc;
					dcc.setHttpAuthn(true);
					dcc.setHttpUser(conf.getValue(PropertiesBasedConfiguration.CFG_SERVER_USERNAME));
					dcc.setHttpPassword(conf.getValue(PropertiesBasedConfiguration.CFG_SERVER_PASSWORD));
					log.debug("Authenticating to Unity with username/password.");
			}
			fetcher = new SAMLAttributeFetcher(conf, cc);
			isEnabled = true;
		} catch (Exception e)
		{
			log.error("Error in VO subsystem configuration (PULL mode): {}. PULL MODE WILL BE DISABLED", e.toString());
		}

		initFinal(log, SAMLAttributeFetcher.ALL_PULLED_ATTRS_KEY, false);
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo)
			throws IOException
	{
		if (!isEnabled)
			throw new SubsystemUnavailableException("Attribute source "+name+" is disabled");
		
		if(!cb.isOK())
			throw new SubsystemUnavailableException("Attribute source "+name+" is temporarily unavailable");
		
		try {
			fetcher.authorise(tokens);
		}catch(SAMLValidationException sve) {}
		
		@SuppressWarnings("unchecked")
		Map<String, List<ParsedAttribute>> allAttributes = (Map<String, List<ParsedAttribute>>) 
				tokens.getContext().get(SAMLAttributeFetcher.ALL_PULLED_ATTRS_KEY);
		List<ParsedAttribute> serviceAttributesOrig = allAttributes.get(conf.getAttributeQueryServiceURL());
		List<ParsedAttribute> serviceAttributes;
		if (serviceAttributesOrig != null)
			serviceAttributes = new ArrayList<>(serviceAttributesOrig);
		else
			serviceAttributes = new ArrayList<>();
		
		return assembleAttributesHolder(serviceAttributes, otherAuthoriserInfo, 
				conf.isPulledGenericAttributesEnabled());
	}
	
	private void checkConnection() {
		if(!isEnabled) {
			status = Status.NOT_APPLICABLE;
			statusMessage = "n/a (not enabled)";
		}
		else {
			final SecurityTokens st = new SecurityTokens();
			String dn = Client.ANONYMOUS_CLIENT_DN;
			try {
				dn = kernel.getContainerSecurityConfiguration().getCredential().getSubjectName();
			}catch(Exception e) {}
			st.setUserName(dn);
			st.setConsignorTrusted(true);

			ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
			Callable<String> check = new Callable<>() {
				public String call() throws Exception {
					try {
						fetcher.authorise(st);
						return "OK";
					}catch(SAMLErrorResponseException sre) {
						return "OK";
					}
					catch(Exception sre) {
						return Log.createFaultMessage("ERROR", sre);
					}
				}
			};
			String result = TimeoutRunner.compute(check, ts, 3000);
			if ("OK".equals(result)) {
				statusMessage = "OK [" + name
						+ " connected to " + fetcher.getServerURL() + "]";
				status = Status.OK;
				cb.OK();
			}
			else{
				statusMessage = "CAN'T CONNECT" + " ["+(result!=null ? result : "")+"]";
				status = Status.DOWN;
				cb.notOK(statusMessage);
			}
		}
	}

	@Override
	public String getConnectionStatusMessage(){
		checkConnection();
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus(){
		checkConnection();
		return status;
	}

	@Override
	public String getExternalSystemName(){
		return name +" Attribute Source " + fetcher.getSimpleAddress();
	}

	protected void initConfig(Logger log, String name)
	{
		this.name = name;
		try
		{
			conf = new PropertiesBasedConfiguration(configFile);
		} catch (IOException e)
		{
			throw new ConfigurationException("Can't read configuration of the SAML subsystem: " +
					e.getMessage());
		}
		isEnabled = true;
	}
	
	protected void initFinal(Logger log, String key, boolean pushMode)
	{
		log.info("Adding SAML attributes callbacks");
		AttributesCallback callback = new AttributesCallback(key, name);
		kernel.getSecurityManager().addCallback(callback);
		
		UnicoreAttributeMappingDef []initializedMappings = Utils.fillMappings(
				conf.getSourceProperties(), Utils.mappings, log);
		if (log.isDebugEnabled())
			log.debug(Utils.createMappingsDesc(initializedMappings));
		specialAttrsHandler = new UnicoreAttributesHandler(conf, initializedMappings, pushMode);
	}
	
	protected SubjectAttributesHolder assembleAttributesHolder(List<ParsedAttribute> serviceAttributes, 
			SubjectAttributesHolder otherAuthoriserInfo, boolean addGeneric)
	{
		SubjectAttributesHolder ret = new SubjectAttributesHolder(otherAuthoriserInfo.getPreferredVos());
		String preferredScope = Utils.handlePreferredVo(otherAuthoriserInfo.getPreferredVos(), 
				conf.getScope(), otherAuthoriserInfo.getSelectedVo());
		UnicoreIncarnationAttributes uia = specialAttrsHandler.extractUnicoreAttributes(
				serviceAttributes, preferredScope, true);
		
		if (addGeneric)
		{		
			List<XACMLAttribute> xacmlAttributes = XACMLAttributesExtractor.getSubjectAttributes(
					serviceAttributes, conf.getScope());
			if (xacmlAttributes != null)
				ret.setXacmlAttributes(xacmlAttributes);
		}
		if (uia.getDefaultAttributes() != null && uia.getValidAttributes() != null)
			ret.setAllIncarnationAttributes(uia.getDefaultAttributes(), uia.getValidAttributes());
		
		//preferred scope is for sure subscope of our scope or our scope. But we are not sure if the 
		// user is really a member of the preferred scope. If not we are not setting the preferred VO at all
		// even as we are sure that the list of attributes is empty (there should be no selected VO at all).
		if (uia.getDefaultVoAttributes() != null && preferredScope != null && ret.getValidIncarnationAttributes() != null) 
		{
			String []usersVos = ret.getValidIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_VOS);
			if (usersVos != null)
			{
				for (String userVo: usersVos)
					if (userVo.equals(preferredScope)) 
					{
						ret.setPreferredVoIncarnationAttributes(preferredScope, 
								uia.getDefaultVoAttributes());
						break;
					}
			}
		}
		return ret;
	}

	public void setConfigurationFile(String configFile)
	{
		this.configFile = configFile;
	}
	
	public String getConfigurationFile()
	{
		return configFile;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public String toString() {
		return getName()+" "+fetcher.getSimpleAddress();
	}

}




