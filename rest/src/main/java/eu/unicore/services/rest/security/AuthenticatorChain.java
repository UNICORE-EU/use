package eu.unicore.services.rest.security;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.ISubSystem;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.KernelInjectable;
import de.fzj.unicore.wsrflite.utils.Utilities;
import eu.unicore.security.SecurityTokens;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyGroupHelper;

public class AuthenticatorChain implements IAuthenticator, ISubSystem {
	
	private static final Logger logger=Log.getLogger(Log.SECURITY, AuthenticatorChain.class);

	private final List<IAuthenticator>chain = new ArrayList<IAuthenticator>();
	
	private final Kernel kernel;
	
	public AuthenticatorChain(Kernel k){
		this.kernel = k;
		registerConfigDefaults();
		k.setAttribute(AuthenticatorChain.class, this);
	}
	
	public void init(Kernel k){}
	
	private final static Collection<String> s = new HashSet<String>();
	
	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
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
				Log.logException("Error using authenticator <"+a.getClass().getName()+">", ex, logger);
			}
		}
		return haveAuth;
	}
	
	public void configure(String name, RESTSecurityProperties properties) throws ConfigurationException {
		String dotName = RESTSecurityProperties.PROP_AUTHN_PREFIX + "." + name + ".";
		String clazz = properties.getValue(dotName + "class");
		
		if (clazz==null) {
			clazz = getDefaultClass(name);
			if(clazz==null)
			throw new ConfigurationException("Inconsistent REST authentication chain definition: " +
					"expected <"+dotName+"class> property with IAuthenticator implementation.");
		}
		try{
			IAuthenticator auth = (IAuthenticator)(Class.forName(clazz).getConstructor().newInstance());
			configureAuth(RESTSecurityProperties.PREFIX+dotName, properties.rawProperties, auth);
			chain.add(auth);
			s.addAll(auth.getAuthSchemes());
			logger.info("Enabled REST authentication: "+auth);
		}
		catch(Exception e){
			throw new ConfigurationException("Cannot create IAuthenticator instance  <"+clazz+">",e);
		}
	}
	
	private void configureAuth(String dotName, Properties properties, IAuthenticator auth) {
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
	
	private final List<AuthenticatorDefaults>defaults = new ArrayList<>();
	
	// lookup and register service factories from classpath
	private void registerConfigDefaults() {
		ServiceLoader<AuthenticatorDefaults> sl=ServiceLoader.load(AuthenticatorDefaults.class);
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
	
	
	
}