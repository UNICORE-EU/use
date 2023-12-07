package eu.unicore.services.rest.security;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyGroupHelper;

public class AuthenticatorChain implements IAuthenticator, ISubSystem {
	
	private static final Logger logger=Log.getLogger(Log.SECURITY, AuthenticatorChain.class);

	private List<IAuthenticator>chain = new ArrayList<>();

	private final Collection<ExternalSystemConnector> connectors = new ArrayList<>();

	private RESTSecurityProperties sp;

	public static synchronized IAuthenticator get(Kernel k) {
		IAuthenticator auth = k.getAttribute(IAuthenticator.class);
		if(auth==null){
			auth = new AuthenticatorChain(k);
			k.setAttribute(IAuthenticator.class, auth);
			k.register((ISubSystem)auth);
		}
		return auth;
	}

	public AuthenticatorChain(Kernel k){
		registerConfigDefaults();
		reloadConfig(k);
	}

	@Override
	public void reloadConfig(Kernel k){
		List<IAuthenticator>newChain = new ArrayList<>();
		connectors.clear();
		sp = new RESTSecurityProperties(k.getContainerProperties().getRawProperties());
		k.setAttribute(RESTSecurityProperties.class, sp);
		String order = sp.getValue(RESTSecurityProperties.PROP_ORDER);
		if(order==null) {
			newChain.add(new RESTSecurityProperties.NullAuthenticator());
		}
		else {
			String[] authNames=order.split(" +");
			for(String authName : authNames){
				IAuthenticator auth = configure(authName, sp, k);
				newChain.add(auth);
				authSchemes.addAll(auth.getAuthSchemes());
				if(auth instanceof ExternalSystemConnector) {
					connectors.add((ExternalSystemConnector)auth);
				}
				logger.info("Enabled REST authentication: {}", auth);
			}
		}
		chain = newChain;
	}
	
	private final static Collection<String> authSchemes = new HashSet<>();
	
	@Override
	public final Collection<String>getAuthSchemes(){
		return authSchemes;
	}
	
	@Override
	public boolean authenticate(Message message, SecurityTokens tokens) {
		boolean haveAuth = false;
		for(IAuthenticator a : chain){
			try{
				haveAuth = a.authenticate(message, tokens) || haveAuth;
				if(tokens.getEffectiveUserName()!=null){
					break;
				}
			}catch(Exception ex){
				logger.debug("Error using authenticator <{}>: {}",
						a.getClass().getName(),
						Log.getDetailMessage(ex));
			}
		}
		return haveAuth;
	}
	
	private static Map<String,String>aliases = new HashMap<>();
	static {
		aliases.put("eu.unicore.uftp.authserver.authenticate.SSHKeyAuthenticator",
					SSHKeyAuthenticator.class.getName());
	}

	private IAuthenticator configure(String name, RESTSecurityProperties properties, Kernel kernel) throws ConfigurationException {
		String dotName = RESTSecurityProperties.PROP_AUTHN_PREFIX + "." + name + ".";
		String clazz = properties.getValue(dotName + "class");
		if (clazz==null) {
			clazz = getDefaultClass(name);
			if(clazz==null)
			throw new ConfigurationException("Inconsistent REST authentication chain definition: " +
					"expected <"+dotName+"class> property with IAuthenticator implementation.");
		}
		clazz = aliases.getOrDefault(clazz, clazz);
		try{
			IAuthenticator auth = (IAuthenticator)(Class.forName(clazz).getConstructor().newInstance());
			configureAuth(RESTSecurityProperties.PREFIX+dotName, properties.rawProperties, auth, kernel);
			return auth;
		}
		catch(Exception e){
			throw new ConfigurationException("Cannot create IAuthenticator instance  <"+clazz+">",e);
		}
	}
	
	private void configureAuth(String dotName, Properties properties, IAuthenticator auth, Kernel kernel) {
		//find parameters for this attribute source
		Map<String,String>params=new PropertyGroupHelper(properties, 
			new String[]{dotName}).getFilteredMap();
		params.remove(dotName+"class");
		Utilities.mapParams(auth,params,RESTSecurityProperties.propsLogger);
		
		//if attribute source provides setProperties method, also pass all properties. Useful 
		//for attribute chains
		Method propsSetter = Utilities.findSetter(auth.getClass(), "properties");
		if (propsSetter != null && propsSetter.getParameterTypes()[0].isAssignableFrom(Properties.class))
		{
			try {
				propsSetter.invoke(auth, new Object[]{properties});
			} catch (Exception e) {
				throw new RuntimeException("Bug: can't set properties on chain: " + e.toString(), e);
			}
		}
		
		// also inject Kernel
		if(auth instanceof KernelInjectable){
			((KernelInjectable)auth).setKernel(kernel);
		}
	}
	
	private final HashSet<AuthenticatorDefaults>defaults = new HashSet<>();
	
	// lookup and register service factories from classpath
	private void registerConfigDefaults() {
		ServiceLoader<AuthenticatorDefaults> sl = ServiceLoader.load(AuthenticatorDefaults.class);
		for (AuthenticatorDefaults defs: sl) {
			defaults.add(defs);
		}
	}
	
	private String getDefaultClass(String authenticatorName) {
		String clazz = null;
		for(AuthenticatorDefaults d: defaults) {
			clazz = d.getImplementationClass(authenticatorName);
			if(clazz!=null)break;
		}
		return clazz;
	}
	
	public List<IAuthenticator>getChain(){
		return Collections.unmodifiableList(chain);
	}

	@Override
	public String getStatusDescription() {
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		if(chain.size()==0) {
			sb.append("N/A");
		}
		for(IAuthenticator auth: chain) {
			sb.append(newline);
			sb.append(" * ").append(auth.toString());
		}
		return sb.toString();
	}

	@Override
	public String getName() {
		return "User authentication";
	}
	
	public RESTSecurityProperties getSecurityProperties() { 
		return sp;
	}
	
}