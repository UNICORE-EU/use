package eu.unicore.services.security;

import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.security.canl.DefaultAuthnAndTrustConfiguration;

/**
 * Simple implementation of {@link IContainerSecurityConfiguration} as java bean.
 * @author K. Benedyczak
 */
public class DefaultContainerSecurityConfiguration extends DefaultAuthnAndTrustConfiguration 
						implements IContainerSecurityConfiguration {
	private boolean sslEnabled;
	private boolean accessControlEnabled;
	private String[] defaultVOs;
	private boolean haveFixedGatewayCert;
	private X509Certificate gatewayCertificate;
	private String pdpConfigurationFile;
	private boolean gatewayEnabled;
	private boolean gatewayWaitingEnabled;
	private int gatewayWaitTime;
	private boolean gatewayRegistrationEnabled;
	private String gatewayRegistrationSecret;
	private int gatewayRegistrationUpdateInterval;
	private X509CertChainValidator trustedAssertionIssuers;
	private boolean sessionsEnabled;
	private long sessionLifetime;
	private int maxSessionsPerUser;
	private boolean isDynamicCredentialReloadEnabled;

	@Override
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}
	
	@Override
	public boolean isAccessControlEnabled() {
		return accessControlEnabled;
	}
	
	public void setAccessControlEnabled(boolean accessControlEnabled) {
		this.accessControlEnabled = accessControlEnabled;
	}

	@Override
	public String[] getDefaultVOs() {
		return defaultVOs;
	}
	
	public void setDefaultVOs(String[] defaultVos) {
		this.defaultVOs = defaultVos;
	}

	@Override
	public boolean isGatewayEnabled() {
		return gatewayEnabled;
	}

	@Override
	public X509Certificate getGatewayCertificate() {
		return gatewayCertificate;
	}
	
	public void setGatewayCertificate(X509Certificate gatewayCertificate) {
		this.gatewayCertificate = gatewayCertificate;
	}

	@Override
	public boolean haveFixedGatewayCertificate() {
		return haveFixedGatewayCert;
	}

	public void setHaveFixedGatewayCertificate(boolean value) {
		this.haveFixedGatewayCert = value;
	}

	public void setGatewayEnabled(boolean gatewayEnabled) {
		this.gatewayEnabled = gatewayEnabled;
	}

	@Override
	public String getPdpConfigurationFile() {
		return pdpConfigurationFile;
	}

	public void setPdpConfigurationFile(String pdpConfigurationFile) {
		this.pdpConfigurationFile = pdpConfigurationFile;
	}

	@Override
	public boolean isGatewayWaitingEnabled() {
		return gatewayWaitingEnabled;
	}

	public void setGatewayWaitingEnabled(boolean gatewayWaitingEnabled) {
		this.gatewayWaitingEnabled = gatewayWaitingEnabled;
	}

	@Override
	public int getGatewayWaitTime() {
		return gatewayWaitTime;
	}

	public void setGatewayWaitTime(int gatewayWaitTime) {
		this.gatewayWaitTime = gatewayWaitTime;
	}

	@Override
	public boolean isGatewayRegistrationEnabled() {
		return gatewayRegistrationEnabled;
	}

	public void setGatewayRegistrationEnabled(boolean gatewayRegistrationEnabled) {
		this.gatewayRegistrationEnabled = gatewayRegistrationEnabled;
	}

	public String getGatewayRegistrationSecret() {
		return gatewayRegistrationSecret;
	}

	public void setGatewayRegistrationSecret(String gatewayRegistrationSecret) {
		this.gatewayRegistrationSecret = gatewayRegistrationSecret;
	}

	@Override
	public int getGatewayRegistrationUpdateInterval() {
		return gatewayRegistrationUpdateInterval;
	}

	public void setGatewayRegistrationUpdateInterval(int gatewayRegistrationUpdateInterval) {
		this.gatewayRegistrationUpdateInterval = gatewayRegistrationUpdateInterval;
	}
	
	/**
	 * Note that this method is currently only delegating to {@link #isAccessControlEnabled()}
	 */
	@Override
	public boolean isAccessControlEnabled(String service) {
		return isAccessControlEnabled();
	}

	@Override
	public boolean isSessionsEnabled(){
		return sessionsEnabled;
	}
	
	public void setSessionsEnabled(boolean sessionsEnabled){
		this.sessionsEnabled=sessionsEnabled;
	}
	
	@Override
	public long getSessionLifetime(){
		return sessionLifetime;
	}
	
	public void setSessionLifetime(long sessionLifetime){
		this.sessionLifetime=sessionLifetime;
	}

	@Override
	public int getMaxSessionsPerUser() {
		return maxSessionsPerUser;
	}

	public void setMaxSessionsPerUser(int maxSessionsPerUser) {
		this.maxSessionsPerUser = maxSessionsPerUser;
	}

	@Override
	public X509CertChainValidator getTrustedAssertionIssuers()
	{
		return trustedAssertionIssuers;
	}
	
	public void setTrustedAssertionIssuers(X509CertChainValidator trustedAssertionIssuers)
	{
		this.trustedAssertionIssuers = trustedAssertionIssuers;
	}

	@Override
	public boolean isDynamicCredentialReloadEnabled() {
		return isDynamicCredentialReloadEnabled;
	}

	public void setDynamicCredentialReloadEnabled(boolean value) {
		this.isDynamicCredentialReloadEnabled = value;
	}
}
