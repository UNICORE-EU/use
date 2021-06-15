/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.vo;

import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.client.SAMLAttributePushOutHandler;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * This handler inserts SAML assertions which are stored in Client's object
 * associated with a request pipeline. If no Client object is found then 
 * no action is taken.
 *  
 * @author golbi
 */
public class SAMLAssertionForwadingHandler extends SAMLAttributePushOutHandler
{
	private static Logger log = Log.getLogger(Log.SECURITY,	SAMLAssertionForwadingHandler.class);
	
	private static final String DO_NOT_FORWARD = SAMLAssertionForwadingHandler.class.getName() + ".doNotPush";
	
	@Override
	public void configure(IClientConfiguration settings)
	{
		//Do nothing. Configuration is determined dynamically at invoke time.
	}
	public void handleMessage(SoapMessage message)
	{
		Client client = AuthZAttributeStore.getClient();
		if (client == null)
		{
			log.debug("No Client object found, not adding SAML assertion.");
			return;
		}
		SecurityTokens tokens = client.getSecurityTokens();
		if (tokens == null)
			return;
		Map<String, Object> secContext = tokens.getContext();
		if (secContext.containsKey(DO_NOT_FORWARD))
			return;
		@SuppressWarnings("unchecked")
		List<Assertion> assertions = (List<Assertion>) secContext.get(
				SAMLAttributePushOutHandler.PUSHED_ASSERTIONS);
		try
		{
			log.debug("Found assertion ");
			convertToJDOM(assertions);
			origList = assertions;
		} catch (Exception e)
		{
			log.error("Error when parsing SAML assertions.", e);
			
		}
		super.handleMessage(message);
	}
}
