/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 22, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.uas.security.vo.conf.IPullConfiguration;
import eu.unicore.uas.security.vo.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Pull UNICORE/X attributes via SAML
 *  
 * @author K. Benedyczak
 */
public class SAMLPullAuthoriser extends SAMLAttributeSourceBase
{
	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, SAMLPullAuthoriser.class);
	private VOAttributeFetcher fetcher;

	@Override
	public void configure(String name) throws ConfigurationException {
		initConfig(log, name);
	}

	@Override
	public void start(Kernel kernel) throws Exception {
		this.kernel=kernel;

		if (!isEnabled)
			return;

		try
		{
			IClientConfiguration cc = kernel.getClientConfiguration().clone();
			if(cc instanceof DefaultClientConfiguration &&
				conf.getValue(PropertiesBasedConfiguration.CFG_VO_SERVICE_USERNAME)!=null){
					DefaultClientConfiguration dcc = (DefaultClientConfiguration)cc;
					dcc.setHttpAuthn(true);
					dcc.setHttpUser(conf.getValue(PropertiesBasedConfiguration.CFG_VO_SERVICE_USERNAME));
					dcc.setHttpPassword(conf.getValue(PropertiesBasedConfiguration.CFG_VO_SERVICE_PASSWORD));
					log.debug("Authenticating to Unity with username/password.");
			}
			fetcher = new VOAttributeFetcher(conf, cc);
		} catch (Exception e)
		{
			isEnabled = false;
			log.error("Error in VO subsystem configuration (PULL mode): " 
				+ e.toString() + "\n PULL MODE WILL BE DISABLED");
		}

		super.initFinal(log, VOAttributeFetcher.ALL_PULLED_ATTRS_KEY, false);
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo)
			throws IOException
	{
		if (!isEnabled)
			throw new IOException("The pull SAML authoriser is disabled");

		fetcher.authorise(tokens);
		
		@SuppressWarnings("unchecked")
		Map<String, List<ParsedAttribute>> allAttributes = (Map<String, List<ParsedAttribute>>) 
				tokens.getContext().get(VOAttributeFetcher.ALL_PULLED_ATTRS_KEY);
		List<ParsedAttribute> serviceAttributesOrig = allAttributes.get(conf.getVOServiceURL());
		List<ParsedAttribute> serviceAttributes;
		if (serviceAttributesOrig != null)
			serviceAttributes = new ArrayList<ParsedAttribute>(serviceAttributesOrig);
		else
			serviceAttributes = new ArrayList<ParsedAttribute>();
		
		return assembleAttributesHolder(serviceAttributes, otherAuthoriserInfo, 
				conf.isPulledGenericAttributesEnabled());
	}

	@Override
	public String getStatusDescription()
	{
		return "SAML Pull Attribute source ["+name+"]: " + (isEnabled ? "OK" : 
			"DISABLED (browse previous log entries for the reason)");
	}
}




