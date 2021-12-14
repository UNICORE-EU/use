package eu.unicore.services.rest.security.jwt;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * JWT configuration. 
 * 
 * UNICORE uses by default the server's private key to sign JWT tokens. 
 * To switch to a shared HMAC secret, simply define a shared secret.
 *  
 * @author schuller
 */
public class JWTProperties extends PropertiesHelper {

	static final Logger propsLogger=Log.getLogger(Log.CONFIGURATION, JWTProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "jwt.";

	public static final String PROP_HMAC_SECRET = "hmacSecret";

	public static final String PROP_TOKEN_LIFETIME = "lifetime";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(PROP_HMAC_SECRET, new PropertyMD().
				setDescription("Shared secret when using the HMAC signing algorithm"));
		META.put(PROP_TOKEN_LIFETIME, new PropertyMD("300").setLong().
				setDescription("Valdity time of issued tokens in seconds"));
	}
	
	public JWTProperties(String prefix, Properties properties) {
		super(prefix, properties, META, propsLogger);
	}
	
	public JWTProperties(Properties properties) {
		super(PREFIX, properties, META, propsLogger);
	}

	public String getHMACSecret(){
		return getValue(PROP_HMAC_SECRET);
	}

	public long getTokenValidity(){
		return getLongValue(PROP_TOKEN_LIFETIME);
	}

}
