package eu.unicore.services.aip.saml.conf;

import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.services.aip.saml.SAMLAttributeFetcher;


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
