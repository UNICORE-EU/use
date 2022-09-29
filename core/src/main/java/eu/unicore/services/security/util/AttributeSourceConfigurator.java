/*
 * Copyright (c) 2009 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2010-04-16
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.services.security.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.services.security.IDynamicAttributeSource;
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
		DEFAULTS.put("FILE", "eu.unicore.uas.security.file.FileAttributeSource");
		DEFAULTS.put("XUUDB", "eu.unicore.uas.security.xuudb.XUUDBAttributeSource");
		DEFAULTS.put("SAML", "eu.unicore.uas.security.saml.SAMLAttributeSource");
		DEFAULTS.put("GRIDMAP-FILE", "eu.unicore.uas.security.gridmapfile.GridMapFileAttributeSource");
		DEFAULTS.put("LDAP", "eu.unicore.uas.security.ldap.LDAPAttributeSource");
	}

	public static IAttributeSource configureAttributeSource(String name, String subPrefix, Properties properties) {
		String dotName = ContainerSecurityProperties.PREFIX + subPrefix + "." + name + ".";
		String clazz = properties.getProperty(dotName + "class");
		clazz = handleOldOrMissingClass(clazz, name);
		if (clazz==null)
			throw new IllegalArgumentException("Inconsistent attribute sources chain definition: " +
					"expected <"+dotName+"class> property with attribute source implementation.");
		logger.debug("Creating attribute source {} served by class <{}>", name, clazz);
		IAttributeSource auth;
		try {
			auth = (IAttributeSource)(Class.forName(clazz).getConstructor().newInstance());
		} catch (Exception e) {
			throw new ConfigurationException("Can't load attribute source implementation, configured as <" +
					clazz + ">: " + e.toString(), e);
		}
		configureCommon(dotName, properties, auth);
		return auth;
	}

	public static IDynamicAttributeSource configureDynamicAttributeSource(String name, String subPrefix, 
			Properties properties) {
		String dotName = ContainerSecurityProperties.PREFIX + subPrefix + "." + name + ".";
		String clazz = properties.getProperty(dotName + "class");
		if (clazz==null)
			throw new IllegalArgumentException("Inconsistent dynamic attribute sources chain definition: " +
					"expected <"+dotName+"class> property with dynamic attribute source implementation.");
		logger.debug("Creating dynamic attribute source {} served by class <{}>", name, clazz);
		IDynamicAttributeSource auth;
		try {
			auth = (IDynamicAttributeSource)(Class.forName(clazz).getConstructor().newInstance());
		} catch (Exception e) {
			throw new ConfigurationException("Can't load dynamic attribute source implementation, configured as <" +
					clazz + ">: " + e.toString(), e);
		}
		configureCommon(dotName, properties, auth);
		return auth;
	}
	
	private static void configureCommon(String dotName, Properties properties, IAttributeSourceBase auth)
	{
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
	}
	
	// accept older attribute source class names
	private static String handleOldOrMissingClass(String clazz, String name) {
		if("eu.unicore.uas.security.xuudb.XUUDBAuthoriser".equals(clazz)) {
			logger.warn("DEPRECATION: found old class name for '"+name+"' attribute source");
			clazz = DEFAULTS.get("XUUDB");
		}
		if("eu.unicore.uas.security.gridmapfile.GridMapFileAuthoriser".equals(clazz)) {
			logger.warn("DEPRECATION: found old class name for '"+name+"' attribute source");
			clazz = DEFAULTS.get("GRIDMAP-FILE");
		}
		if(clazz==null) {
			clazz = DEFAULTS.get(name);
		}
		return clazz;
	}

}
