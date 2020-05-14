package eu.unicore.services.rest.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.ContainerSecurityProperties;
import eu.unicore.security.SecurityTokens;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * configure security (e.g. authentication) for REST services
 *
 * @author schuller
 */
public class RESTSecurityProperties extends PropertiesHelper {
	
	static final Logger propsLogger=Log.getLogger(Log.CONFIGURATION, RESTSecurityProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = ContainerSecurityProperties.PREFIX + "rest.";
	
	/**
	 * base for authentication property names
	 */
	public static final String PROP_AUTHN_PREFIX = "authentication";

	public static final String PROP_ORDER = PROP_AUTHN_PREFIX + ".order";

	public static final String PROP_FORBID_NO_CREDS = PROP_AUTHN_PREFIX + ".failOnMissingAuthCredentials";

	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(PROP_ORDER, new PropertyMD().
				setDescription("Order of authentication components."));
		META.put(PROP_FORBID_NO_CREDS, new PropertyMD("false").setBoolean().
				setDescription("Immediately fail REST calls with no credentials without invoking access control."));
		META.put(PROP_AUTHN_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure authentication. See separate documentation for details."));
		META.put("jwt", new PropertyMD().setCanHaveSubkeys().
				setDescription("Prefix used to configure JWT support. See separate docs."));
	}
	
	private IAuthenticator auth;

	Properties rawProperties;
	
	private final Kernel kernel;
	
	/**
	 * @param kernel
	 * @param p
	 * @throws ConfigurationException
	 */
	public RESTSecurityProperties(Kernel kernel, Properties p) throws ConfigurationException {
		super(PREFIX, p, META, propsLogger);
		this.rawProperties = p;
		this.kernel = kernel;
		createAuth();
	}
	
	protected void createAuth(){
		auth = new NullAuthenticator();
		String order = getValue(PROP_ORDER);
		if(order!=null){
			AuthenticatorChain chain = new AuthenticatorChain(kernel);
			String[] authNames=order.split(" +");
			for(String authName : authNames){
				chain.configure(authName, this);
			}
			auth = chain;
		}
	}
	
	public boolean forbidNoCreds(){
		return getBooleanValue(PROP_FORBID_NO_CREDS);
	}

	public IAuthenticator getAuthenticator(){
		return auth;
	}
	
	public static class NullAuthenticator implements IAuthenticator{
		@Override
		public boolean authenticate(Message message, SecurityTokens tokens) {
			//NOP
			return true;
		}
		
		@SuppressWarnings("unchecked")
		private final static Collection<String> s = Collections.EMPTY_SET;
		
		@Override
		public final Collection<String>getAuthSchemes(){
			return s;
		}
	}
	
}