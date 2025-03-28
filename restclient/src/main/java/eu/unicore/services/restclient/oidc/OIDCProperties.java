package eu.unicore.services.restclient.oidc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * preferences for getting a token from an OIDC token endpoint
 *
 * @author schuller
 */
public class OIDCProperties extends PropertiesHelper {
	
	private static final Logger log = Log.getLogger(Log.CONFIGURATION, OIDCProperties.class);

	public static final String PREFIX = "oidc.";

	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	
	public static final String TOKEN_ENDPOINT = "endpoint";
	public static final String CLIENT_ID = "clientID";
	public static final String CLIENT_SECRET = "clientSecret";
	public static final String AUTH_MODE = "authentication";
	public static final String GRANT_TYPE = "grantType";
	public static final String SCOPE = "scope";
	public static final String OTP = "otp";
	public static final String OTP_PARAM_NAME = "request_key_for_otp";
	public static final String STORE_REFRESH_TOKEN = "storeRefreshToken";
	public static final String REFRESH_TOKEN_FILENAME = "refreshTokenFile";
	public static final String REFRESH_INTERVAL = "refreshInterval";

	public static enum AuthMode {
		BASIC, POST,
	}

	public static final Map<String, PropertyMD> META = new HashMap<>();
	static
	{
		META.put(TOKEN_ENDPOINT, new PropertyMD().setMandatory().setDescription("The OIDC server endpoint for requesting a token"));
		
		META.put(USERNAME, new PropertyMD().setDescription("Username used to log in. If not given in " +
															"configuration, it will be asked interactively."));
		META.put(PASSWORD, new PropertyMD().setSecret().setDescription("Password used to log in. It is suggested " +
				"not to use this option for security reasons. If not given in configuration, " +
				"it will be asked interactively."));
		META.put(OTP, new PropertyMD().setDescription("Additional one-time password for two-factor authentication. "
				+ "Set this to 'QUERY' to query it interactively."));
		META.put(OTP_PARAM_NAME, new PropertyMD("otp").
				setDescription("(internal) How to send the OTP value to the server."));
		META.put(STORE_REFRESH_TOKEN, new PropertyMD("true").setBoolean().
				setDescription("Whether to store the refresh token between client invocations."));
		META.put(REFRESH_TOKEN_FILENAME, new PropertyMD().
				setDescription("Filename for storing the refresh token between client invocations."));
		META.put(REFRESH_INTERVAL, new PropertyMD("300").setInt().
				setDescription("Interval (seconds) before refreshing the token."));
		META.put(CLIENT_ID, new PropertyMD().setDescription("Client ID for authenticating to the OIDC server."));
		META.put(CLIENT_SECRET, new PropertyMD().setDescription("Client secret for authenticating to the OIDC server."));
		META.put(GRANT_TYPE, new PropertyMD("password").setDescription("Grant type to request."));
		META.put(AUTH_MODE, new PropertyMD().setEnum(AuthMode.BASIC)
				.setDescription("How to authenticate (i.e. send client id/secret) to the OIDC server (BASIC or POST)."));
		META.put(SCOPE, new PropertyMD().setDescription("Token scope to request from the OIDC server."));
	}

	public OIDCProperties(Properties properties, String prefix) throws ConfigurationException
	{
		super(prefix, properties, META, log);
	}

	public OIDCProperties(Properties properties) throws ConfigurationException
	{
		this(properties, PREFIX);
	}

	public String getUsername() {
		return getValue(USERNAME);
	}

	public String getPassword() {
		return getValue(PASSWORD);
	}

	public boolean queryOTP() {
		return "QUERY".equalsIgnoreCase(getValue(OTP));
	}

	public String getOTP() {
		return getValue(OTP);
	}

	public String getOTPParamName() {
		return getValue(OTP_PARAM_NAME);
	}

	public String getGrantType() {
		return getValue(GRANT_TYPE);
	}

	public String getScope() {
		return getValue(SCOPE);
	}

	public String getClientID() {
		return getValue(CLIENT_ID);
	}

	public String getClientSecret() {
		return getValue(CLIENT_SECRET);
	}

	public String getTokenEndpoint() {
		return getValue(TOKEN_ENDPOINT);
	}

	public boolean isStoreRefreshTokens() {
		return getBooleanValue(STORE_REFRESH_TOKEN);
	}

	public AuthMode getAuthMode() {
		return getEnumValue(AUTH_MODE, AuthMode.class);
	}

	public String getRefreshTokensFilename() {
		String filename = getValue(REFRESH_TOKEN_FILENAME);
		if(filename==null && getBooleanValue(STORE_REFRESH_TOKEN)) {
			filename = System.getProperty("user.home")+File.separator+".ucc"
					+File.separator+"refresh-tokens";
		}
		return filename;
	}

	public int getRefreshInterval() {
		return getIntValue(REFRESH_INTERVAL);
	}

}
