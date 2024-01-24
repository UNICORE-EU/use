package eu.unicore.services.aip.saml.conf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

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


/**
 * Standard implementation class for the both pull and push modules configurations,
 * based on a properties file. 
 * 
 * @author K. Benedyczak
 */
public class PropertiesBasedConfiguration extends FilePropertiesHelper implements IPullConfiguration
{
	private static final Logger log = Log.getLogger(IBaseConfiguration.LOG_PFX, PropertiesBasedConfiguration.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "saml.";
	
	public static final String DEF_CONFIG_FILE_PATH = "conf/saml.config";

	public static final String CFG_INCARNATION_ATTR_PFX = "unicoreAttribute."; 
	
	public static final String CFG_SCOPE = "group"; 

	public static final String CFG_LOCAL_SERVER_URI = "localServerURI";
	
	public static final String CFG_ATTRIBUTE_QUERY_URL = "attributeQueryURL";
	public static final String CFG_SERVER_USERNAME = "attributeQuery.username";
	public static final String CFG_SERVER_PASSWORD = "attributeQuery.password";
	
	public static final String CFG_ENABLE_GENERIC_ATTRIBUTES = "enableGenericAttributes";
	public static final String CFG_CACHE_TTL = "cacheTtl";
	public static final String CFG_VERIFY_SIGNATURES = "verifySignatures";
		
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> DEFAULTS = new HashMap<>();
	static 
	{
		DEFAULTS.put(CFG_ENABLE_GENERIC_ATTRIBUTES, new PropertyMD("true").
				setDescription("If turned on, then not only the recognized UNICORE attributes are processed, but also all others, which can be used for authorization."));
		DEFAULTS.put(CFG_SCOPE, new PropertyMD().
				setDescription("Group which is accepted by this attribute source. "+
						"UNICORE/X will honor only attributes with exactly this scope or global (i.e. without scope set)"));
		DEFAULTS.put(CFG_LOCAL_SERVER_URI, new PropertyMD().
				setDescription("Can contain a local server SAML identifier to be used in SAML requests." +
						" If unset, then the server's X.500 DN is used."));
		DEFAULTS.put(CFG_CACHE_TTL, new PropertyMD("600").
				setDescription("Controls pulled attributes cache. Set to negative integer to disable the caching or to positive number - lifetime in seconds of cached entries."));
		DEFAULTS.put(CFG_ATTRIBUTE_QUERY_URL, new PropertyMD("localhost").
				setDescription("Full address (URL) of SAML Attribute Query service."));
		DEFAULTS.put(CFG_VERIFY_SIGNATURES, new PropertyMD("true").
				setDescription("Additional security for the pulled assertions (except transport level which is always on) can be achieved by verification of signatures of the received assertions. The key which is used for verification must be present in the SAML truststore."));
		DEFAULTS.put(CFG_SERVER_USERNAME, new PropertyMD().
				setDescription("If certificate-based authentication to the SAML server is disabled, you might be able to use username/password. This sets the username."));
		DEFAULTS.put(CFG_SERVER_PASSWORD, new PropertyMD().
				setDescription("If certificate-based authentication to the SAML server is disabled, you might be able to use username/password. This sets the password."));
		DEFAULTS.put(TruststoreProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties starting with this prefix are used to configure validation of SAML assertion issuers certificates. Trust anchors should contain only the trusted SAML servers certificates. All options are the same as those for other UNICORE truststores."));
		DEFAULTS.put(CFG_INCARNATION_ATTR_PFX, new PropertyMD().setCanHaveSubkeys().
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
		initPull();
	}

	private void initPull()
	{
		try
		{
			new URI(getAttributeQueryServiceURL());
		} catch (URISyntaxException e)
		{
			throw new ConfigurationException("Value of server URI (" + 
				getAttributeQueryServiceURL()  + ") is not a valid URI.");
		}
		
		log.info("VO PULL authorization is enabled. " +
				"Will use VO service with address: " + getAttributeQueryServiceURL());
		
	}
	
	private static String getConfigFile(String preferred)
	{
		String cfgFile = preferred;
		if (cfgFile == null)
			cfgFile = DEF_CONFIG_FILE_PATH;
		return cfgFile;
	}
	
	@Override
	public int getCacheTtl()
	{
		return getIntValue(CFG_CACHE_TTL);
	}

	@Override
	public String getAttributeQueryServiceURL()
	{
		return getValue(CFG_ATTRIBUTE_QUERY_URL);
	}

	@Override
	public boolean isPulledSignatureVerficationEnabled()
	{
		return getBooleanValue(CFG_VERIFY_SIGNATURES);
	}

	@Override
	public boolean isPulledGenericAttributesEnabled()
	{
		return getBooleanValue(CFG_ENABLE_GENERIC_ATTRIBUTES);
	}
	
	@Override
	public String getScope()
	{
		return getValue(CFG_SCOPE);
	}

	@Override
	public String getLocalServerURI()
	{
		return getValue(CFG_LOCAL_SERVER_URI);
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
