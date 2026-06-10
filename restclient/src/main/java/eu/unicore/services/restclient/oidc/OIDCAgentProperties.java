package eu.unicore.services.restclient.oidc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Settings for configuring the authentication via oidc-agent
 *
 * @author schuller
 */
public class OIDCAgentProperties extends PropertiesHelper {

	private static final Logger log = Log.getLogger(Log.CONFIGURATION, OIDCAgentProperties.class);

	public static final String PREFIX = "oidc-agent.";
	
	public static final String ACCOUNT = "account";
	public static final String SCOPE = "scope";
	public static final String LIFETIME = "lifetime";
	public static final String REFRESH_INTERVAL = "refreshInterval";

	public static final Map<String, PropertyMD> META = new HashMap<>();
	static
	{
		META.put(ACCOUNT, new PropertyMD().setMandatory().setDescription("Account short name."));
		META.put(SCOPE, new PropertyMD().setDescription("OpenID scope(s) to request."));
		META.put(LIFETIME, new PropertyMD().setInt().setPositive().
				setDescription("Minimum lifetime of the issued access token."));
		META.put(REFRESH_INTERVAL, new PropertyMD("300").setInt().
				setDescription("Interval (seconds) before refreshing the token."));
	}

	public OIDCAgentProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

	public String getAccount() {
		return getValue(ACCOUNT);
	}
}
