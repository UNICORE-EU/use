/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on May 13, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.saml.conf;

import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.uas.security.saml.SAMLAttributeFetcher;


/**
 * Interface used to obtain all common configuration option required by {@link SAMLAttributeFetcher} 
 * @author K. Benedyczak
 */
public interface IBaseConfiguration
{
	/**
	 * For log4j logging
	 */
	static final String LOG_PFX = "unicore.security.saml";

	/**
	 * @return URI identifying the server.
	 */
	public String getLocalServerURI();

	/**
	 * @return scope / group of attributes that we honor
	 */
	public String getScope();

	/**
	 * @return validator used to validate certificates of assertion issuers (note: this 
	 * is nearly always a different validator from the one used for SSL connections!) 
	 */
	public SamlTrustChecker getAssertionIssuerValidator();
}
