/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.wsrflite.security;

import java.util.List;

import javax.security.auth.x500.X500Principal;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.etd.TrustDelegation;
import eu.unicore.util.httpclient.ETDClientSettings;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Utility methods which allow for easy configuration of {@link IClientConfiguration} 
 * with ETD assertions that shall be sent. This is typically used
 * when invoking a request on user's behalf in context of a longer processing pipline. 
 * @author golbi
 */
public class ETDAssertionForwarding
{
	/**
	 * Configures {@link IClientConfiguration} to forward ETD assertions which are in the
	 * client object. Use it when you are sure that target service doesn't require 
	 * original user's rights. 
	 * @param client ETD assertions to be forwarded are taken from this object
	 * @param properties properties to be updated with ETD settings.
	 * @return true if ETD insertion was set, false if client doesn't provide ETD assertions
	 * so nothing can be done.
	 */
	public static boolean configureETD(Client client, IClientConfiguration properties)
	{
		SecurityTokens tokens = client.getSecurityTokens();
		if (tokens == null)
			return false;
		List<TrustDelegation> tds = tokens.getTrustDelegationTokens();
		if (tds == null || tds.size() == 0)
			return false;
		ETDClientSettings etdSettings = properties.getETDSettings();
		
		etdSettings.setExtendTrustDelegation(false);
		etdSettings.setTrustDelegationTokens(tds);
		etdSettings.setRequestedUser(tokens.getEffectiveUserName());
		return true;
	}
	
	/**
	 * Configures {@link IClientConfiguration} to forward ETD assertions which are in the
	 * client object. The original chain will be extended by an assertion delegating
	 * the trust to the receiver, so it will be also allowed to work on original user's 
	 * behalf.
	 * @param client ETD assertions to be forwarded are taken from this object
	 * @param config client configuration to be updated with ETD settings.
	 * @param receiver receiver of the new assertion (i.e. identity of the server where the request will be sent).
	 * @return true if ETD insertion was set, false if client doesn't provide ETD assertions
	 * so nothing can be done.
	 */
	public static boolean configureETDChainExtension(Client client, IClientConfiguration config,
			X500Principal receiver)
	{
		boolean ret = configureETD(client, config);
		if (!ret)
			return false;
		ETDClientSettings etdSettings = config.getETDSettings();
		etdSettings.setExtendTrustDelegation(true);
		etdSettings.setIssuerCertificateChain(config.getCredential().getCertificateChain());
		etdSettings.setReceiver(receiver);
		return true;
	}
}
