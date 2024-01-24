package eu.unicore.services.security;

import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;

/**
 * Interface extends {@link IAuthnAndTrustConfiguration} with container security 
 * settings: 
 * <ul>
 *  <li> access control settings (PDP)
 *  <li> attribute information points settings (AIP)
 *  <li> gateway integration (whether signatures on gw assertions are required etc)
 *  <li> default VOs 
 * </ul>
 * Also the local credential and trust settings are accessible as this interface extends 
 * {@link IAuthnAndTrustConfiguration}.
 * <p>
 * None of the methods can return null, unless explicitly stated in method description.
 *  
 * @author K. Benedyczak
 */
public interface IContainerSecurityConfiguration extends IAuthnAndTrustConfiguration {
	
	public static final long SAML_VALIDITY_GRACE_TIME = 75*60*1000;
	
	/**
	 * Returns true if SSL mode is enabled.
	 */
	public boolean isSslEnabled();
	
	/**
	 * @return do we check access on the web service level (using PDP)? 
	 */
	public boolean isAccessControlEnabled();

	/**
	 * @return do we check access on the web service level (using PDP) for a particular service? 
	 */
	public boolean isAccessControlEnabled(String service);

	/**
	 * @return the PDP configuration file path
	 */
	public String getPdpConfigurationFile();

	/**
	 * @return array of default VOs in preference order. Might be empty.
	 */
	public String[] getDefaultVOs();
	
	/**
	 * @return whether Consignor assertions (produced by the gateway) should
	 * processed or ignored.
	 */
	public boolean isGatewayAuthnEnabled();
	
	/**
	 * @return whether signatures of Consignor assertions (produced by the gateway) should
	 * are required and if are checked. Relevant only if {@link #isGatewayAuthnEnabled()} returns true.
	 */
	public boolean isGatewaySignatureCheckingEnabled();
	
	/**
	 * @return the Gateway's certificate. May be null only if 
	 * {@link #isGatewayAuthnEnabled()} or {@link #isGatewaySignatureCheckingEnabled()} returns false or 
	 * early during startup when gateway's dn was not yet discovered.
	 */
	public X509Certificate getGatewayCertificate();
	
	/**
	 * @return whether to wait for gateway on startup. This allows also to discover automatically gateway's DN.
	 */
	public boolean isGatewayWaitingEnabled();
	
	/**
	 * @return for how long to wait for gateway on startup
	 */
	public int getGatewayWaitTime();
	
	/**
	 * @return true if gateway cert is hardcoded via a file
	 */
	public boolean haveFixedGatewayCertificate();

	/**
	 * @return whether to autoregister with gateway
	 */
	public boolean isGatewayRegistrationEnabled();
	
	/**
	 * @return gateway auto registration update interval
	 */
	public int getGatewayRegistrationUpdateInterval();

	/**
	 * returns validator used as to (optionally) validate SAML assertions
	 * received from authentication providers
	 *
	 * @return truststore 
	 */
	public X509CertChainValidator getTrustedAssertionIssuers();
	
	/**
	 * whether security sessions are enabled
	 */
	public boolean isSessionsEnabled();
	
	/**
	 * session lifetime in seconds
	 */
	public long getSessionLifetime();
	
	/**
	 * maximum number of sessions per user (i.e. DN+ClientIP). If exceeded, some cleanup 
	 * will be performed.
	 */
	public int getMaxSessionsPerUser();

	public boolean isDynamicCredentialReloadEnabled();
}
