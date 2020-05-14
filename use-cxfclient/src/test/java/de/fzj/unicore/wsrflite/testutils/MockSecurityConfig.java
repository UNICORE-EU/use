/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on May 28, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package de.fzj.unicore.wsrflite.testutils;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;


/**
 * @author K. Benedyczak
 */
public class MockSecurityConfig extends DefaultClientConfiguration
{
	public static final String KS = "src/test/resources/client/client.jks";
	public static final String KS_PASSWD = "the!client";

	public static final String KS_ALIAS = "mykey";
	public static final String KS_ALIAS_GW = "gw";
	public static final String KS_ALIAS_WRONG = "mykey_wrong";
	
	private boolean correctSSLAuthN;
	
	
	public MockSecurityConfig(boolean doSSLAuthN, boolean correctSSLAuthN, boolean logMsg, boolean forceMTOM) throws Exception
	{
		this(doSSLAuthN,correctSSLAuthN);
		setMessageLogging(logMsg);
		if(forceMTOM)getExtraSecurityTokens().put("FORCE_MTOM", Boolean.TRUE);
	}
	
	public MockSecurityConfig(boolean doSSLAuthN, boolean correctSSLAuthN) throws Exception
	{
		setSslEnabled(true);
		setDoSignMessage(true);
		setSslAuthn(doSSLAuthN);
		this.correctSSLAuthN = correctSSLAuthN;
		setCredential(new KeystoreCredential(KS, 
				KS_PASSWD.toCharArray(), 
				KS_PASSWD.toCharArray(), 
				getKeystoreAlias(), 
				"JKS"));
		setValidator(new KeystoreCertChainValidator(KS, 
				KS_PASSWD.toCharArray(), 
				"JKS", 
				-1));
		setMaxWSRetries(3);
	}
	
	private String getKeystoreAlias()
	{
		if (correctSSLAuthN)
			return KS_ALIAS;
		return KS_ALIAS_WRONG;
	}

	public MockSecurityConfig clone()
	{
		try
		{
			MockSecurityConfig ret = new MockSecurityConfig(doSSLAuthn(), correctSSLAuthN);
			cloneTo(ret);
			return ret;
		} catch (Exception e)
		{
			throw new RuntimeException("Can't clone", e);
		}
	}
}
