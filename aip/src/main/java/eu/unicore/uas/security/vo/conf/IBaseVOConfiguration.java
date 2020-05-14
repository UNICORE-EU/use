/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on May 13, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo.conf;

import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.uas.security.vo.VOAttributeFetcher;


/**
 * Interface used to obtain all common configuration option required by {@link VOAttributeFetcher} 
 * @author K. Benedyczak
 */
public interface IBaseVOConfiguration
{
	/**
	 * For log4j logging
	 */
	static final String _LOG_PFX = "unicore.security.vo";

	/**
	 * For log4j logging
	 */
	public static final String LOG_PFX = _LOG_PFX + "common";

	/**
	 * @return Optional scope which will limit the attributes which 
	 * server accepts. Server will honour only attributes with exactly 
	 * this scope or global (i.e. without scope set).
	 */
	public String getScope();
	
	/**
	 * @return URI identifying the server.
	 */
	public String getServerURI();
	
	/**
	 * @return URI identifying the VO server.
	 */
	public String getVOServiceURI();

	/**
	 * @return validator used to validate certificates of assertion issuers (note: this 
	 * is nearly always a different validator from the one used for SSL connections!) 
	 */
	public SamlTrustChecker getAssertionIssuerValidator();
}
