package eu.unicore.services.aip.saml.conf;

import eu.unicore.services.aip.saml.SAMLAttributeFetcher;



/**
 * Interface used to obtain configuration options required by 
 * {@link SAMLAttributeFetcher}. The security configuration for the VO client
 * is provided via another interface.
 * 
 * @author K. Benedyczak
 */
public interface IPullConfiguration extends IBaseConfiguration
{

	/**
	 * @return Whether to pass normal (not role or xlogin) attributes.
	 */
	public boolean isPulledGenericAttributesEnabled();

	/**
	 * @return How long should the attributes be cached, in seconds. Negative
	 * value disables the cache.
	 */
	public int getCacheTtl();

	/**
	 * @return The VO server's URL for queries
	 */
	public String getAttributeQueryServiceURL();

	/**
	 * @return If the received assertions should be additionally digitally signed
	 * and this signature verified.
	 */
	public boolean isPulledSignatureVerficationEnabled();

}
