package eu.unicore.services.security.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyGroupHelper;

/**
 * Utility class used to configure attribute source
 * 
 * @author schuller
 * @author golbi
 */
public class AttributeSourceConfigurator {

	private static final Logger logger=Log.getLogger(Log.SECURITY,AttributeSourceConfigurator.class);
	
	final static Map<String,String> DEFAULTS = new HashMap<>();

	static {
		DEFAULTS.put("FILE", "eu.unicore.services.aip.file.FileAttributeSource");
		DEFAULTS.put("XUUDB", "eu.unicore.services.aip.xuudb.XUUDBAttributeSource");
		DEFAULTS.put("SAML", "eu.unicore.services.aip.saml.SAMLAttributeSource");
		DEFAULTS.put("GRIDMAP-FILE", "eu.unicore.services.aip.gridmapfile.GridMapFileAttributeSource");
		DEFAULTS.put("LDAP", "eu.unicore.services.aip.ldap.LDAPAttributeSource");
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends IAttributeSourceBase> T configureAS(String name, String subPrefix, 
			Properties properties) {
		String dotName = ContainerSecurityProperties.PREFIX + subPrefix + "." + name + ".";
		String clazz = handleOldOrMissingClass(properties.getProperty(dotName + "class"), name);
		if (clazz==null)
			throw new IllegalArgumentException("Inconsistent (dynamic) attribute sources chain definition: " +
					"expected <"+dotName+"class> property with (dynamic) attribute source implementation.");
		logger.debug("Creating (dynamic) attribute source {} served by class <{}>", name, clazz);
		try {
			T auth = (T)(Class.forName(clazz).getConstructor().newInstance());
			//find parameters for this attribute source
			Map<String,String>params=new PropertyGroupHelper(properties, 
					new String[]{dotName}).getFilteredMap();
			params.remove(dotName+"class");
			Utilities.mapParams(auth,params,logger);

			//if attribute source provides setProperties method, also pass all properties. Useful 
			//for attribute chains
			Method propsSetter = Utilities.findSetter(auth.getClass(), "properties");
			if (propsSetter != null && propsSetter.getParameterTypes()[0].
					isAssignableFrom(Properties.class))
				try {
					propsSetter.invoke(auth, new Object[]{properties});
				} catch (Exception e) {
					throw new RuntimeException("Bug: can't set properties on chain: " + e.toString(), e);
				}
			return auth;
		} catch (Exception e) {
			throw new ConfigurationException("Can't load dynamic attribute source implementation, configured as <" +
					clazz + ">: " + e.toString(), e);
		}
	}
	
	// accept older attribute source class names
	private static String handleOldOrMissingClass(String clazz, String name) {
		if(clazz==null) {
			return DEFAULTS.get(name);
		}
		boolean updated = false;
		if("eu.unicore.uas.security.xuudb.XUUDBAuthoriser".equals(clazz)) {
			clazz = DEFAULTS.get("XUUDB");
			updated = true;
		}
		if("eu.unicore.uas.security.gridmapfile.GridMapFileAuthoriser".equals(clazz)) {
			clazz = DEFAULTS.get("GRIDMAP-FILE");
			updated = true;
		}
		String oldPrefix = "eu.unicore.uas.security";
		if(clazz.startsWith(oldPrefix)) {
			clazz = "eu.unicore.services.pdp" + clazz.substring(oldPrefix.length());
			updated = true;
		}
		if(updated) {
			logger.warn("DEPRECATION: found old class name for '{}' attribute source, superseded by <{}>",
					name, clazz);
		}
		return clazz;
	}

}
