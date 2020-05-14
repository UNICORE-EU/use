/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 22, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo.conf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.TruststoreBasedSamlTrustChecker;
import eu.unicore.security.canl.LoggingStoreUpdateListener;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.FilePropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;


/**
 * Standard implementation class for the both pull and push modules configurations,
 * based on a properties file. 
 * 
 * @author K. Benedyczak
 */
public class PropertiesBasedConfiguration extends FilePropertiesHelper implements IPullConfiguration
{
	private static final Logger log = Log.getLogger(IBaseVOConfiguration.LOG_PFX, PropertiesBasedConfiguration.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "vo.";
	
	public static final String DEF_CONFIG_FILE_PATH = "conf/vo.config";
	public static final String ENV_CONFIG_FILE = "pl.edu.icm.uasvo.configFile";

	public static final String CFG_INCARNATION_ATTR_PFX = "unicoreAttribute."; 

	public static final String CFG_VO_SERVICE_SCOPE = "group";
	public static final String CFG_VO_SERVICE_URI = "voServerURI";
	public static final String CFG_DISABLE_PULL_WHEN_PUSHED = "pull.disableIfAttributesWerePushed";
	public static final String CFG_PULL_ENABLE = "pull.enable";
	public static final String CFG_PULL_GENERIC_ENABLE = "pull.enableGenericAttributes";
	public static final String CFG_SERVER_URI = "localServerURI";
	public static final String CFG_VO_SERVICE_URL = "pull.voServerURL";
	public static final String CFG_VO_PULL_CACHE = "pull.cacheTtl";
	public static final String CFG_VO_VERIFY_SIGNATURES = "pull.verifySignatures";
	public static final String CFG_PUSH_ENABLE = "push.enable";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> DEFAULTS = new HashMap<String, PropertyMD>();
	static 
	{
		DocumentationCategory pullCat = new DocumentationCategory("Pull mode");
		DocumentationCategory pushCat = new DocumentationCategory("Push mode");
		DocumentationCategory mainCat = new DocumentationCategory("General");
		
		DEFAULTS.put(CFG_DISABLE_PULL_WHEN_PUSHED, new PropertyMD("true").setCategory(pullCat).
				setDescription("Whether pull mode should be skipped if user sent (or pushed) some attributes with the request. Note that to make this feature work PUSH mode must be enabled AND PULL authorization must be invoked AFTER PUSH authorization."));
		DEFAULTS.put(CFG_PULL_ENABLE, new PropertyMD("false").setCategory(pullCat).
				setDescription("Defines if pull mode should be enabled."));
		DEFAULTS.put(CFG_PULL_GENERIC_ENABLE, new PropertyMD("true").setCategory(pullCat).
				setDescription("If turned on, then not only the recognized UNICORE attributes are processed, but also all others, which can be used for authorization."));
		DEFAULTS.put(CFG_SERVER_URI, new PropertyMD().setCategory(pullCat).
				setDescription("Can contain this, local server SAML identifier, to be used in SAML requests in PULL mode." +
						" If unset then DN identity is used for queries, created from the local server's credential."));
		DEFAULTS.put(CFG_VO_PULL_CACHE, new PropertyMD("600").setCategory(pullCat).
				setDescription("Controls pulled attributes cache. Set to negative integer to disable the caching or to positive number - lifetime in seconds of cached entries."));
		DEFAULTS.put(CFG_VO_SERVICE_URL, new PropertyMD("localhost").setCategory(pullCat).
				setDescription("Full address (URL) of SAML VO service. Note that this server's CA cert must be present in the main truststore of the server to create the connection."));
		DEFAULTS.put(CFG_VO_VERIFY_SIGNATURES, new PropertyMD("true").setCategory(pullCat).
				setDescription("Additional security for the pulled assertions (except transport level which is always on) can be achieved by verification of signatures of the received assertions. The key which is used for verification must be present in the VO truststore."));

		DEFAULTS.put(CFG_PUSH_ENABLE, new PropertyMD("false").setCategory(pushCat).
				setDescription("Defines if push mode should be enabled."));
		
		DEFAULTS.put(CFG_VO_SERVICE_SCOPE, new PropertyMD().setCategory(mainCat).
				setDescription("VO or group which is accepted by this attribute source. " +
						"Server will honour only attributes with exactly this scope or global (i.e. without scope set)." +
						"This will work only with legacy UVOS service, for Unity configure proper scope at server side."));
		DEFAULTS.put(CFG_VO_SERVICE_URI, new PropertyMD().setCategory(mainCat).
				setDescription("Identification URI of the VO service providing attribtues for this source. Only attributes issued by this issuer will be honoured."));
		DEFAULTS.put(TruststoreProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().setCategory(mainCat).
				setDescription("Properties starting with this prefix are used to configure validation of VO assertion issuers certificates. Trust anchors should contain only the trusted VO servers certificates. All options are the same as those for other UNICORE truststores."));
		DEFAULTS.put(CFG_INCARNATION_ATTR_PFX, new PropertyMD().setCanHaveSubkeys().setCategory(mainCat).
				setDescription("Properties starting with this prefix are used to configure mappings of SAML attributes to UNICORE internal ones."));
	}

	private SamlTrustChecker trustChecker;
	
	public PropertiesBasedConfiguration() throws FileNotFoundException, IOException
	{
		this(null);
	}
	
	public PropertiesBasedConfiguration(String configFile) 
		throws FileNotFoundException, IOException
	{
		super(PREFIX, getConfigFile(configFile), DEFAULTS, log);
		TruststoreProperties trustProperties = new TruststoreProperties(properties, 
				Collections.singleton(new LoggingStoreUpdateListener()), 
				PREFIX+TruststoreProperties.DEFAULT_PREFIX);
		trustChecker = new TruststoreBasedSamlTrustChecker(trustProperties.getValidator());
		if (isPullEnabled() && getVOServiceURI() == null)
			throw new ConfigurationException("For the VO subsystem, the " + 
					getKeyDescription(CFG_VO_SERVICE_URI) + " property must be defined.");

		if (!isPullEnabled())
			log.info("VO PULL authorization is disabled.");
		else
			initPull();
	}

	private void initPull()
	{
		if (getVOServiceURL() == null)
			throw new ConfigurationException("The " + getKeyDescription(CFG_VO_SERVICE_URL) + 
					" is required if the pull mode is enabled.");
		try
		{
			new URI(getVOServiceURL());
		} catch (URISyntaxException e)
		{
			throw new ConfigurationException("Value of server URI (" + 
				getVOServiceURL()  + ") is not a valid URI.");
		}
		
		log.info("VO PULL authorization is enabled. " +
				"Will use VO service with address: " + getVOServiceURL());
		
	}
	
	private static String getConfigFile(String preferred)
	{
		String cfgFile = preferred;
		if (cfgFile == null)
			cfgFile = System.getProperty(ENV_CONFIG_FILE);
		if (cfgFile == null)
			cfgFile = DEF_CONFIG_FILE_PATH;
		return cfgFile;
	}
	
	@Override
	public int getChacheTtl()
	{
		return getIntValue(CFG_VO_PULL_CACHE);
	}

	@Override
	public String getVOServiceURL()
	{
		return getValue(CFG_VO_SERVICE_URL);
	}

	@Override
	public boolean isPullEnabled()
	{
		return getBooleanValue(CFG_PULL_ENABLE);
	}
	
	@Override
	public String getVOServiceURI()
	{
		return getValue(CFG_VO_SERVICE_URI);
	}
	
	@Override
	public boolean isPulledSignatureVerficationEnabled()
	{
		return getBooleanValue(CFG_VO_VERIFY_SIGNATURES);
	}

	@Override
	public boolean isPulledGenericAttributesEnabled()
	{
		return getBooleanValue(CFG_PULL_GENERIC_ENABLE);
	}

	@Override
	public String getScope()
	{
		return getValue(CFG_VO_SERVICE_SCOPE);
	}

	@Override
	public String getServerURI()
	{
		return getValue(CFG_SERVER_URI);
	}

	@Override
	public boolean disableIfAttributesWerePushed()
	{
		return getBooleanValue(CFG_DISABLE_PULL_WHEN_PUSHED);
	}

	@Override
	public SamlTrustChecker getAssertionIssuerValidator()
	{
		return trustChecker;
	}
	
	public Properties getSourceProperties()
	{
		return properties;
	}
}
