/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

package eu.unicore.services.security;

import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.services.security.pdp.UnicoreXPDP;

/**
 * Interface extends {@link IAuthnAndTrustConfiguration} with container security 
 * settings: 
 * <ul>
 *  <li> access control settings (PDP)
 *  <li> attribute information points settings (AIP)
 *  <li> gateway integration (whether signatures on gw assertions are required etc)
 *  <li> checking of digital signatures on certain requests
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
	 * @return the configured PDP instance, null can be returned only if {@link #isAccessControlEnabled()}
	 * returns false
	 */
	public UnicoreXPDP getPdp();

	/**
	 * @return the PDP configuration file path
	 */
	public String getPdpConfigurationFile();
	
	/**
	 * @return the configured attribute source instance
	 */
	public IAttributeSource getAip();

	/**
	 * @return the configured dynamic attribute source instance
	 */
	public IDynamicAttributeSource getDap();

	/**
	 * @return array of default VOs in preference order. Might be empty.
	 */
	public String[] getDefaultVOs();
	
	/**
	 * @return do we require signatures on certain messages?
	 */
	public boolean isSigningRequired();
	
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
	 * @return gateway autoregistration update interval
	 */
	public int getGatewayRegistrationUpdateInterval();
	
	/**
	 * (XSEDE integration requirment)
	 * @return Object used to verify certificate chains of ETD assertions
	 */
	public X509CertChainValidator getETDValidator();

	/**
	 * Since U7: returns validator used as truststore with certificates of trusted issuers of bootstrap ETD.
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
}
