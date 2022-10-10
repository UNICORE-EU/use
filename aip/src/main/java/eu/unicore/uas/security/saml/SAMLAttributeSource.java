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
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.uas.security.saml.conf.IPullConfiguration;
import eu.unicore.uas.security.saml.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Pull UNICORE/X attributes via SAML
 *  
 * @author K. Benedyczak
 */
public class SAMLAttributeSource extends SAMLAttributeSourceBase implements ExternalSystemConnector
{
	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, SAMLAttributeSource.class);

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

		super.initFinal(log, SAMLAttributeFetcher.ALL_PULLED_ATTRS_KEY, false);
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

}




