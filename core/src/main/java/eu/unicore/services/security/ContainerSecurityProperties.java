/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.ICM file for licensing information.
 */ 

package eu.unicore.services.security;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.canl.AuthnAndTrustProperties;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.security.canl.LoggingStoreUpdateListener;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.security.pdp.AcceptingPdp;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.AttributeSourcesChain;
import eu.unicore.services.security.util.DynamicAttributeSourcesChain;
import eu.unicore.services.security.util.BaseAttributeSourcesChain.MergeLastOverrides;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * USE security configuration implementation using a {@link Properties} source.
 * 
 * @author K. Benedyczak
 */
public class ContainerSecurityProperties extends DefaultContainerSecurityConfiguration {

	private static final Logger logger=Log.getLogger(Log.SECURITY, ContainerSecurityProperties.class);
	private static final Logger propsLogger=Log.getLogger(Log.CONFIGURATION, ContainerSecurityProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = ContainerProperties.PREFIX + "security.";
	
	/**
	 * property defining whether SSL is enabled
	 */
	public static final String PROP_SSL_ENABLED = "sslEnabled";

	/**
	 * do we check access? 
	 */
	public static final String PROP_CHECKACCESS = "accesscontrol";

	/**
	 * access control PDP class name 
	 * (implementing <code>de.fzj.uas.security.XacmlPDP</code>)
	 */
	public static final String PROP_CHECKACCESS_PDP = "accesscontrol.pdp";
	
	/**
	 * configuration file for the PDP
	 */
	public static final String PROP_CHECKACCESS_PDPCONFIG = "accesscontrol.pdpConfig";
	
	
	/**
	 * do we honor gateway assertions?
	 */
	public final static String PROP_GATEWAY_AUTHN = "gateway.enable";
	
	/**
	 * do we check if the consignor assertion is signed?
	 */
	@Deprecated
	public final static String PROP_CHECK_CONSIGNOR_SIGNATURE = "gateway.checkSignature";

	/**
	 * for stricter security, define a the gateway's certificate as a path to a DER or PEM file 
	 */
	public static final String PROP_GATEWAY_CERT = "gateway.certificate";

	/**
	 * Enable registration with a gateway
	 */
	public static final String PROP_AUTOREGISTER_WITH_GATEWAY = "gateway.registration";

	public static final String PROP_AUTOREGISTER_WITH_GATEWAY_SECRET = "gateway.registrationSecret";

	/**
	 * update interval for the registration with a gateway in seconds (default =
	 * 30 second)
	 */
	public static final String PROP_AUTOREGISTER_WITH_GATEWAY_UPDATE = "gateway.registrationUpdateInterval";

	/**
	 * To enable waiting for the gateway on startup, set this property to 'true'
	 */
	public static final String PROP_GATEWAY_WAIT = "gateway.waitOnStartup";

	/**
	 * To enable waiting for the gateway on startup, set this property to the
	 * max wait time in seconds default: 180
	 */
	public static final String PROP_GATEWAY_WAITTIME = "gateway.waitTime";

	/**
	 * do we require signatures on certain messages 
	 */
	@Deprecated
	public static final String PROP_REQUIRE_SIGNATURES = "signatures";

	/**
	 * list of default VOs, in preference order 
	 */
	public static final String PROP_DEFAULT_VOS = "defaultVOs.";
	
	/**
	 * base for AIP property names
	 */
	public static final String PROP_AIP_PREFIX = "attributes";
	
	/**
	 * attribute sources order property
	 */
	public static final String PROP_AIP_ORDER = PROP_AIP_PREFIX + ".order";

	/**
	 * property for defining the combining policy if multiple sources are used
	 */
	public static final String PROP_AIP_COMBINING_POLICY = PROP_AIP_PREFIX + ".combiningPolicy";
	
	/**
	 * base for DAP property names
	 */
	public static final String PROP_DAP_PREFIX = "dynamicAttributes";
	
	/**
	 * dynamic attribute sources order property
	 */
	public static final String PROP_DAP_ORDER = PROP_DAP_PREFIX + ".order";

	/**
	 * property for defining the combining policy if multiple DAPs are used
	 */
	public static final String PROP_DAP_COMBINING_POLICY = PROP_DAP_PREFIX + ".combiningPolicy";

	public static final String PROP_TRUSTED_ASSERTION_ISSUERS_PFX = "trustedAssertionIssuers.";
	
	public static final String PROP_ADDITIONAL_ACCEPTED_SAML_IDS = "additionalServiceIdentifier";
	
	/**
	 * property for enabling/disabling security sessions
	 */
	public static final String PROP_SESSIONS_ENABLED = "sessionsEnabled";

	/**
	 * property for defining the session lifetime in seconds
	 */
	public static final String PROP_SESSIONS_LIFETIME = "sessionLifetime";
	
	/**
	 * property for defining the max number of sessions per user
	 */
	public static final String PROP_SESSIONS_PERUSER = "sessionsPerUser";
	
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();
	static
	{
		META.put(PROP_SSL_ENABLED, new PropertyMD("true").
				setDescription("Controls whether secure SSL mode is enabled."));
		META.put(PROP_CHECKACCESS, new PropertyMD("true").setUpdateable().setCanHaveSubkeys().
				setDescription("Controls whether access checking (authorisation) is enabled. Can be used per service after adding dot and service name to the property key."));
		META.put(PROP_CHECKACCESS_PDP, new PropertyMD().setClass(UnicoreXPDP.class).
				setDescription("Controls which Policy Decision Point (PDP, the authorisation engine) should be used. Default value is determined as follows: if eu.unicore.uas.pdp.local.LocalHerasafPDP is available then it is used. If not then this option becomes mandatory."));
		META.put(PROP_CHECKACCESS_PDPCONFIG, new PropertyMD().setPath().
				setDescription("Path of the PDP configuration file"));
		META.put(PROP_DEFAULT_VOS, new PropertyMD("").setList(true).
				setDescription("List of default VOs, which should be assigned for a request without " +
						"a VO set. The first VO on the list where the user is member will be used."));
		META.put(PROP_GATEWAY_AUTHN, new PropertyMD("true").
				setDescription("Whether to accept gateway-based authentication. Note that if it is enabled " +
						"either the site must be secured (usually via firewall) to disable " +
						"non-gateway access or the verification of gateway's assertions must be enabled."));
		META.put(PROP_CHECK_CONSIGNOR_SIGNATURE, new PropertyMD("true").
				setDeprecated().setDescription("(deprecated)"));
		META.put(PROP_GATEWAY_CERT, new PropertyMD().setPath().
				setDescription("Path to gateway's certificate file in PEM or DER format. " +
					"Note that DER format is used only for files with '.der' extension. It is used " +
					"only for gateway's authentication assertions verification (if enabled). " +
					"Note that this is not needed to set it if waiting for gateway on startup is turned on."));
		META.put(PROP_AUTOREGISTER_WITH_GATEWAY, new PropertyMD("false").
				setDescription("Whether the site should try to autoregister itself with the Gateway. " +
						"This must be also configured on the Gateway side."));
		META.put(PROP_AUTOREGISTER_WITH_GATEWAY_SECRET, new PropertyMD().
				setDescription("Required secret when autoregistering with the Gateway. " +
						"This must match the secret configured on the Gateway side."));
		META.put(PROP_AUTOREGISTER_WITH_GATEWAY_UPDATE, new PropertyMD("30").setMin(10).
				setDescription("How often the automatic gateway registration should be refreshed."));
		META.put(PROP_GATEWAY_WAITTIME, new PropertyMD("180").setPositive().
				setDescription("Controls for how long to wait for the gateway on startup (in seconds)."));
		META.put(PROP_GATEWAY_WAIT, new PropertyMD("true").
				setDescription("Controls whether to wait for the gateway at startup."));
		META.put(PROP_REQUIRE_SIGNATURES, new PropertyMD("false").
				setDeprecated().setDescription("(deprecated)"));
		META.put(PROP_AIP_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Prefix used for configurations of particular attribute sources."));
		META.put(PROP_AIP_ORDER, new PropertyMD().setDescription("Attribute sources in invocation order."));
		META.put(PROP_AIP_COMBINING_POLICY, new PropertyMD(MergeLastOverrides.NAME).
				setDescription("What algorithm should be used for combining the attributes from " +
						"multiple attribute sources (if more then one is defined)."));
		META.put(PROP_DAP_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Prefix used for configurations of particular dynamic attribute sources."));
		META.put(PROP_DAP_ORDER, new PropertyMD().setDescription("Dynamic attribute sources in invocation order."));
		META.put(PROP_DAP_COMBINING_POLICY, new PropertyMD(MergeLastOverrides.NAME).
				setDescription("What algorithm should be used for combining the attributes from " +
						"multiple dynamic attribute sources (if more then one is defined)."));
		META.put(PROP_TRUSTED_ASSERTION_ISSUERS_PFX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Allows for configuring a truststore " +
						"(using normal truststore properties with this prefix) with certificates of trusted services (not CAs!) which are permitted to issue trust delegations and authenticate with SAML. Typically this truststore should contain certificates of all Unity instanes installed."));
		META.put(PROP_ADDITIONAL_ACCEPTED_SAML_IDS, new PropertyMD().setList(false).
				setDescription("List of additional service identifiers (e.g. URLs where this service is accessible) "
						+ "accepted in SAML authentication."));

		META.put(TruststoreProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure container's trust settings and certificates validation. See separate documentation for details."));
		META.put(CredentialProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure the credential used by the container. See separate documentation for details."));
		
		META.put(PROP_SESSIONS_ENABLED, new PropertyMD("true").
				setDescription("Controls whether the server supports security sessions which reduce client/server traffic and load."));
		META.put(PROP_SESSIONS_LIFETIME, new PropertyMD("28800").setInt().setPositive().
				setDescription("Controls the lifetime of security sessions (in seconds)."));
		META.put(PROP_SESSIONS_PERUSER, new PropertyMD("5").setInt().setPositive().
				setDescription("Controls the number of security sessions each user can have. If exceeded, some cleanup will be performed."));
		META.put("rest", new PropertyMD().setCanHaveSubkeys().
				setDescription("Prefix used to configure REST subsystem security. See separate docs."));
		
	}
	
	private PropertiesHelper properties;
	private AuthnAndTrustProperties authAndTrustProperties = null;
	
	public ContainerSecurityProperties(Properties p) throws ConfigurationException {
		this(p, null);
	}

	public ContainerSecurityProperties(Properties source, IAuthnAndTrustConfiguration authAndTrust) 
			throws ConfigurationException {
		backwardsCompat(source);
		properties = new PropertiesHelper(PREFIX, source, META, propsLogger);
		setSslEnabled(properties.getBooleanValue(PROP_SSL_ENABLED));
		setAccessControlEnabled(properties.getBooleanValue(PROP_CHECKACCESS));
		setSigningRequired(properties.getBooleanValue(PROP_REQUIRE_SIGNATURES));
		setGatewayAuthnEnabled(properties.getBooleanValue(PROP_GATEWAY_AUTHN));

		boolean credNeeded = isSslEnabled();
		boolean trustNeeded = isSslEnabled() || isSigningRequired() || isGatewayAuthnEnabled();
		
		if (authAndTrust == null) {
			authAndTrustProperties = new AuthnAndTrustProperties(source, PREFIX+TruststoreProperties.DEFAULT_PREFIX, 
					PREFIX + CredentialProperties.DEFAULT_PREFIX, 
					!trustNeeded, !credNeeded);
			authAndTrust = authAndTrustProperties;
		}
		setValidator(authAndTrust.getValidator());
		setCredential(authAndTrust.getCredential());

		setETDValidator(getValidator());

		try
		{
			TruststoreProperties assertionTrustProperties = new TruststoreProperties(source, 
					Collections.singleton(new LoggingStoreUpdateListener()), 
					PREFIX+PROP_TRUSTED_ASSERTION_ISSUERS_PFX);
			setTrustedAssertionIssuers(assertionTrustProperties.getValidator());
			logger.info("Loaded trusted SAML assertion issuers truststore, " +
					"with the following trusted service certificates:");
			for (X509Certificate c: assertionTrustProperties.getValidator().getTrustedIssuers())
				logger.info("  -) " + X500NameUtils.getReadableForm(c.getSubjectX500Principal()));
		} catch (ConfigurationException e)
		{
			logger.info("Trusted SAML assertion issuers truststore is not defined! " +
					"Validating Unity assertions will not work.");
			setTrustedAssertionIssuers(new BinaryCertChainValidator(false));
		}
		
		List<String> defaultVos = properties.getListOfValues(PROP_DEFAULT_VOS); 
		setDefaultVOs(defaultVos.toArray(new String[0]));
		
		if (isGatewayAuthnEnabled()) {
			logger.info("Enabling gateway support");
			setGatewayRegistrationEnabled(properties.getBooleanValue(PROP_AUTOREGISTER_WITH_GATEWAY));
			if (isGatewayRegistrationEnabled()) {
				setGatewayRegistrationUpdateInterval(properties.getIntValue(
						PROP_AUTOREGISTER_WITH_GATEWAY_UPDATE));
				setGatewayRegistrationSecret(properties.getValue(PROP_AUTOREGISTER_WITH_GATEWAY_SECRET));
			}
			setGatewayWaitingEnabled(properties.getBooleanValue(PROP_GATEWAY_WAIT));
			if (isGatewayWaitingEnabled())
				setGatewayWaitTime(properties.getIntValue(PROP_GATEWAY_WAITTIME));

			String certFile = properties.getValue(PROP_GATEWAY_CERT);
			if (certFile != null) {
				setGatewayCertificate(loadGatewayCertificate(certFile));
				setHaveFixedGatewayCertificate(true);
			}
		} else {
			logger.info("Gateway support is turned OFF globally, none of gateway settings will be used.");
			setGatewayRegistrationEnabled(false);
			setGatewayWaitingEnabled(false);
		}
		
		setSessionsEnabled(properties.getBooleanValue(PROP_SESSIONS_ENABLED));
		setSessionLifetime(properties.getLongValue(PROP_SESSIONS_LIFETIME));
		setMaxSessionsPerUser(properties.getIntValue(PROP_SESSIONS_PERUSER));
		
		setAip(createAttributeSource(source));
		setDap(createDynamicAttributeSource(source));
		setPdp(createPDP(properties));
	}
	
	// backwards compatibility fixes
	protected void backwardsCompat(Properties properties) throws ConfigurationException {
		String old = "de.fzj.unicore.wsrflite";
		for(String key: properties.stringPropertyNames()){
			String val = properties.getProperty(key);
			if(!val.startsWith(old))continue;
			String newValue = "eu.unicore.services"+val.substring(old.length());
			properties.put(key, newValue);
			logger.warn("Old property value <{}> is DEPRECATED, superseded by <{}>", val, newValue);
		}
	}

	public void updateProperties(Properties newProperties) {
		properties.setProperties(newProperties);
		if (authAndTrustProperties != null && authAndTrustProperties.getTruststoreProperties() != null)
			authAndTrustProperties.getTruststoreProperties().setProperties(newProperties);
		if (getValidator() == null && isAccessControlEnabled()) {
			logger.warn("Can't enable access control without truststore settings.");
			setAccessControlEnabled(false);
		}
			
		//TODO - with this only few truststore settings and accessControl enabled/disabled are 
		//updateable at runtime. Implement more (but be careful to check overall consistency!
		//Note: if PDP configuration updates will be done here remember to also update the code in UAS
		// which will need to reinit them.
	}
	
	private X509Certificate loadGatewayCertificate(String certFile) {
		Encoding encoding = certFile.endsWith(".der") ? Encoding.DER : Encoding.PEM;
		try (InputStream is = new BufferedInputStream(new FileInputStream(certFile))){
			return CertificateUtils.loadCertificate(is, encoding);
		} catch (IOException e) {
			throw new ConfigurationException("Can not load the gateway's certificate '" + 
					certFile +"'", e);
		}
	}
	
	private IAttributeSource createAttributeSource(Properties raw) 
			throws ConfigurationException {
		String order = properties.getValue(PROP_AIP_ORDER); 
		if (order == null) {
			logger.info("No attribute source is defined in the configuration, " +
					"users won't have any authorisation attributes assigned");
			return new NullAttributeSource();
		}
		AttributeSourcesChain ret = new AttributeSourcesChain();
		ret.setCombiningPolicy(properties.getValue(PROP_AIP_COMBINING_POLICY));
		ret.setOrder(order);
		ret.setProperties(raw);
		ret.configure(null);
		return ret;
	}

	private IDynamicAttributeSource createDynamicAttributeSource(Properties raw) 
			throws ConfigurationException {
		String order = properties.getValue(PROP_DAP_ORDER); 
		if (order == null) {
			logger.info("No dynamic attribute source is defined in the configuration, " +
					"users won't have any dynamic incarnation attributes assigned");
			return new NullAttributeSource();
		}
		
		logger.debug("Creating the main dynamic attribute sources chain");
		DynamicAttributeSourcesChain ret = new DynamicAttributeSourcesChain();
		ret.setCombiningPolicy(properties.getValue(PROP_DAP_COMBINING_POLICY));
		ret.setOrder(order);
		ret.setProperties(raw);
		ret.configure(null);
		return ret;
	}

	
	@SuppressWarnings("unchecked")
	private UnicoreXPDP createPDP(PropertiesHelper properties) throws ConfigurationException {
		if (!isAccessControlEnabled())
			return new AcceptingPdp();
			
		if (properties.isSet(PROP_CHECKACCESS_PDPCONFIG)) {
			String conf=properties.getValue(PROP_CHECKACCESS_PDPCONFIG);
			setPdpConfigurationFile(conf);
		}
		
		Class<? extends UnicoreXPDP> pdpClazz = properties.getClassValue(PROP_CHECKACCESS_PDP, UnicoreXPDP.class);
		//this is as use-pdp is not available at compile time and still we want to provide a sensible default for admins.
		if (pdpClazz == null) {
			try {
				pdpClazz = (Class<? extends UnicoreXPDP>) Class.forName("eu.unicore.uas.pdp.local.LocalHerasafPDP");
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException("The default eu.unicore.uas.pdp.local.LocalHerasafPDP PDP is not available and PDP was not configured.");
			}
		}
		try {
			Constructor<? extends UnicoreXPDP> constructor = pdpClazz.getConstructor();
			logger.info("Using PDP class <"+pdpClazz.getName()+">");
			UnicoreXPDP pdp = constructor.newInstance();
			return pdp;
		}catch(Exception e) {
			throw new ConfigurationException("Can't create a PDP.", e);
		}
	}

	@Override
	public boolean isAccessControlEnabled(String service) {
		return properties.getSubkeyBooleanValue(PROP_CHECKACCESS, service);
	}
	
	public List<String>getAdditionalSAMLIds(){
		return properties.getListOfValues(PROP_ADDITIONAL_ACCEPTED_SAML_IDS);
	}

}
