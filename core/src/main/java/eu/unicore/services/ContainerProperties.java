/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package eu.unicore.services;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.persistence.Persistence;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Provides an RW access to the base USE {@link Kernel} configuration.
 * The configuration is based on {@link Properties} source.
 * 
 * @author K. Benedyczak
 */
public class ContainerProperties extends PropertiesHelper {

	private static final Logger log = Log.getLogger(Log.CONFIGURATION, ContainerProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "container.";

	/** property defining the host (e.g. "localhost") */
	public static final String SERVER_HOST = "host";

	/** property defining the port (e.g. 7777) */
	public static final String SERVER_PORT = "port";

	/**
	 * property defining the base container URL where clients can access the container
	 * (for example "http://localhost:7777/"). 	If using a gateway (or other type of frontend), 
	 * this will be the frontend's address with the sitename appended. 
	 *  
	 */
	public static final String EXTERNAL_URL = "externalurl";

	/**
	 * property defining the base URL where SOAP services can be contacted (for
	 * example "http://localhost:7777/services"). 
	 * This should NOT end with a slash "/". This is the address that is 
	 * published in registries, etc.
	 */
	@Deprecated
	public static final String WSRF_BASEURL = "baseurl";


	/** property defining the Servlet context path (usually "/services") */
	public static final String WSRF_SERVLETPATH = "servletpath";

	/**
	 * To override the default USE startup code, set this to a list of names
	 * of classes implementing java.lang.Runnable
	 */
	public static final String ON_STARTUP_KEY = "onstartup";

	/**
	 * Alternative way to provide on startup runnables is to use a list of properties.
	 */
	public static final String ON_STARTUP_LIST_KEY = ON_STARTUP_KEY+".";

	/**
	 * whether to run a connections check after startup (defaults to "true")
	 */
	public static final String ON_STARTUP_SELFTEST = "onstartupSelftest";

	/**
	 * property defining the vsite name
	 */
	public static final String VSITE_NAME_PROPERTY = "sitename";


	/**
	 * property key for setting the core thread pool size for the 
	 * scheduled execution service
	 */
	public static final String CORE_POOL_SIZE="resources.scheduled.size";

	/**
	 * property key for setting the timeout in millis for removing idle threads
	 */
	public static final String POOL_TIMEOUT="resources.scheduled.idletime";

	/**
	 * property key for setting the minimum thread pool size for the 
	 * scheduled execution service
	 */
	public static final String EXEC_CORE_POOL_SIZE="resources.executor.minsize";

	/**
	 * property key for setting the maximum thread pool size for the 
	 * scheduled execution service
	 */
	public static final String EXEC_MAX_POOL_SIZE="resources.executor.maxsize";

	/**
	 * property key for setting the timeout in millis for removing idle threads
	 */
	public static final String EXEC_POOL_TIMEOUT="resources.executor.idletime";

	/**
	 * property defining which class to use as Persist implementation
	 */
	public static final String WSRF_PERSIST_CLASSNAME = "wsrf.persistence.persist";

	/**
	 * property for limiting the number of service instances per user. Attach the service name.
	 * E.g., to limit JobManagement service instances to 1000 per user, set
	 * <code>
	 * unicore.maxInstancesPerUser.JobManagement=1000
	 * </code>
	 */
	public final static String MAX_INSTANCES="wsrf.maxInstancesPerUser";

	/**
	 * property name for configuring the initial delay for ws-resource expiry checking
	 * set to an integer value (seconds)
	 */
	public static final String EXPIRYCHECK_INITIAL="wsrf.expirycheck.initial";

	/**
	 * property name for configuring the repeat period for ws-resource expiry checking
	 * set to an integer value (seconds)
	 */
	public static final String EXPIRYCHECK_PERIOD="wsrf.expirycheck.period";

	/**
	 * property name for configuring the default lifetime (in seconds)
	 * set to an integer value (seconds)
	 */
	public static final String DEFAULT_LIFETIME="wsrf.lifetime.default";

	/**
	 * property name for configuring the maximum lifetime (in seconds)
	 * set to an integer value (seconds)
	 */
	public static final String MAXIMUM_LIFETIME="wsrf.lifetime.maximum";

	/**
	 * property name for configuring the timeout when attempting to lock a resource
	 * set to an integer value (seconds)
	 */
	public static final String INSTANCE_LOCKING_TIMEOUT="wsrf.instanceLockingTimeout";

	/** Property defining the default termination time of service group entries in seconds */
	public static final String WSRF_SGENTRY_TERMINATION_TIME = "wsrf.sg.defaulttermtime";

	// registry related
	public static final String EXTERNAL_REGISTRY_USE = "externalregistry.use";
	public static final String EXTERNAL_REGISTRY_KEY = "externalregistry.url";

	/**
	 * prefix for feature configuration
	 */
	public static final String FEATURE_KEY = "feature.";

	public static final String LOGGING_KEY = "messageLogging.";


	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(ON_STARTUP_SELFTEST, new PropertyMD("true").
				setDescription("Controls whether to run tests of connections to external services on startup."));
		META.put(VSITE_NAME_PROPERTY, new PropertyMD("DEMO-SITE").setDescription(
				"Short, human friendly, name of the target system, should be unique in the federation."));
		META.put(WSRF_SERVLETPATH, new PropertyMD("/services").
				setDescription("Servlet context path. In most cases shouldn't be changed."));
		META.put(WSRF_BASEURL, new PropertyMD().
				setDescription("(deprecated, use 'container.externalurl') Server URL as visible from the outside, usually the gateway's address, including '<sitename>/services'"));
		META.put(EXTERNAL_URL, new PropertyMD().
				setDescription("Server URL as visible from the outside, usually the gateway's address, including '<sitename>'"));

		META.put(SERVER_HOST, new PropertyMD("localhost").
				setDescription("Server interface to listen on."));
		META.put(SERVER_PORT, new PropertyMD("7777").setBounds(0, 65535).
				setDescription("Server listen port."));

		//TODO Provide proper descriptions and review!!!! Important: are all per service properties marked with setCanHaveSubKeys ?
		META.put(CORE_POOL_SIZE, new PropertyMD("3").setPositive().
				setDescription("Defines the thread pool size for the execution of scheduled services."));
		META.put(POOL_TIMEOUT, new PropertyMD("60000").
				setDescription("Timeout in millis for removing idle threads."));
		META.put(EXEC_CORE_POOL_SIZE, new PropertyMD("2").setPositive().
				setDescription("The minimum thread pool size for the internal execution service"));
		META.put(EXEC_MAX_POOL_SIZE, new PropertyMD("16").
				setDescription("The maximum thread pool size for the internal execution service"));
		META.put(EXEC_POOL_TIMEOUT, new PropertyMD("60000").
				setDescription("The timeout in millis for removing idle threads."));

		META.put(WSRF_PERSIST_CLASSNAME, new PropertyMD(Persistence.class.getName()).
				setDescription("Implementation used to maintain the persistence of resources state."));

		META.put(EXPIRYCHECK_INITIAL, new PropertyMD("120").setCanHaveSubkeys().
				setDescription("The initial delay for resource expiry checking (seconds). Additionally it can be used as a per-service setting, after appending a dot and service name to the property key."));
		META.put(EXPIRYCHECK_PERIOD, new PropertyMD("60").setCanHaveSubkeys().
				setDescription("The interval for resource expiry checking (seconds).  Additionally it can be used as a per-service setting, after appending a dot and service name to the property key."));

		META.put(INSTANCE_LOCKING_TIMEOUT, new PropertyMD("30").setCanHaveSubkeys().
				setDescription("The timeout when attempting to lock resources. Additionally it can be used as a per-service setting, after appending a dot and service name to the property key."));

		META.put(DEFAULT_LIFETIME, new PropertyMD("86400").setPositive().setCanHaveSubkeys().
				setDescription("Default lifetime of resources (in seconds).  Add dot and service name as a suffix of this property to set a default per particular service type."));
		META.put(MAXIMUM_LIFETIME, new PropertyMD().setInt().setPositive().setCanHaveSubkeys().
				setDescription("Maximum lifetime of resources (in seconds).  Add dot and service name as a suffix of this property to set a limit per particular service type."));
		META.put(MAX_INSTANCES, new PropertyMD(""+Integer.MAX_VALUE).setPositive().setCanHaveSubkeys().
				setDescription("Maximum number per user of WS-resource instances. Add dot and service name as a suffix of this property to set a limit per particular service type."));
		META.put(WSRF_SGENTRY_TERMINATION_TIME, new PropertyMD("1800").setPositive().
				setDescription("The default termination time of service group entries in seconds."));

		META.put(ON_STARTUP_KEY, new PropertyMD().
				setDescription("Space separated list of runnables to be executed on server startup." +
						" It is preferred to use " + ON_STARTUP_LIST_KEY));
		META.put(ON_STARTUP_LIST_KEY, new PropertyMD().setList(true).
				setDescription("List of runnables to be executed on server startup."));

		META.put(EXTERNAL_REGISTRY_USE, new PropertyMD("false").setUpdateable().
				setDescription("Whether the service should register itself in external registry(-ies), defined separately."));
		META.put(EXTERNAL_REGISTRY_KEY, new PropertyMD().setList(false).setUpdateable().
				setDescription("List of external registry URLs to register local services."));

		META.put("security.", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure container's security. See separate documentation for details."));
		META.put("client.", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure clients created by the container. See separate documentation for details."));
		META.put("httpServer.", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure container's Jetty HTTP server. See separate documentation for details."));
		META.put("persistence.", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure container's persistence layer. See separate documentation for details."));
		
		META.put("feature.", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure the deployed features. See separate documentation for details."));

		META.put(LOGGING_KEY, new PropertyMD("false").setBoolean().setCanHaveSubkeys().
				setDescription("Append service name and set to 'true' to enable message logging for that service."));
	}

	protected ThreadingServices threadingServices;

	private final boolean sslMode;

	public ContainerProperties(Properties properties, boolean sslMode) throws ConfigurationException {
		super(PREFIX, properties, META, log);
		this.sslMode = sslMode;
		updatePropertiesInternal(sslMode);
		threadingServices = new ThreadingServices(this);
	}

	public ThreadingServices getThreadingServices() {
		return threadingServices;
	}

	public synchronized void setProperties(Properties properties) throws ConfigurationException{
		super.setProperties(properties);
		updatePropertiesInternal(sslMode);
	}

	private void updatePropertiesInternal(boolean sslMode) {
		String frontend = null;
		
		if (!isSet(EXTERNAL_URL) && !isSet(WSRF_BASEURL)) {
			String host = getValue(SERVER_HOST);
			if("0.0.0.0".equals(host)) {
				try{
					host = InetAddress.getLocalHost().getCanonicalHostName();
				}catch(Exception ex) {
					throw new ConfigurationException("Property <"+PREFIX+EXTERNAL_URL+"> is not set.");
				}
			}
			String port = getValue(SERVER_PORT);
			String protocol = sslMode ? "https" : "http";
			String site = getValue(VSITE_NAME_PROPERTY);
			String gwS = getValue("security.gateway.enable");
			if(gwS==null)gwS="true";
			boolean gw = Boolean.parseBoolean(gwS);
			
			frontend = protocol + "://" + host + ":" + port 
					+ (gw ? "/" + site : "");
			setProperty(EXTERNAL_URL, frontend);
			
			String path = getValue(WSRF_SERVLETPATH);
			setProperty(WSRF_BASEURL, frontend+path);

			log.info("Using an autogenerated access URL = "+frontend);
		}

		if (!isSet(WSRF_BASEURL)) {
			String externalUrl = getValue(EXTERNAL_URL);
			String path = getValue(WSRF_SERVLETPATH);
			setProperty(WSRF_BASEURL, externalUrl+path);
		}

		String baseUrl = getValue(WSRF_BASEURL);
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length()-1);
			setProperty(WSRF_BASEURL, baseUrl);
			log.warn("Removed trailing '/' from value of " + PREFIX+WSRF_BASEURL);
		}
		if (!isSet(EXTERNAL_URL)) {
			String externalUrl = baseUrl.split("/services")[0];
			setProperty(EXTERNAL_URL, externalUrl);
			log.info("Using an autogenerated externalurl = "+externalUrl);
		}
	}

	/**
	 * Returns properties of the container. 
	 * <b>IMPORTANT!</b> In the most cases this method should not be used. The only
	 * proper use case of this method in UNICORE is to use the returned object as an input
	 * for the custom configuration class which extends/uses {@link PropertiesHelper}. This
	 * is useful in services based on USE.
	 * @return underlying properties
	 */
	public Properties getRawProperties() {
		return properties;
	}

	/**
	 * Returns getValue(WSRF_BASEURL).
	 * For convenience only - this is very often used.
	 * @return base URL of the container
	 */
	public String getBaseUrl() {
		return getValue(WSRF_BASEURL);
	}

	/**
	 * @return URL of the container as it is accessible from clients, i.e. using
	 * the gateway address and site name if appropriate
	 * (this differs from getBaseUrl() by not including the "/services" bit which is specific
	 * to SOAP services) 
	 */
	public String getContainerURL() {
		return getValue(EXTERNAL_URL);
	}

	// deprecated config keys
	private static final String[]deprecated = {"container.externalregistry.autodiscover",
			"container.registry.globalAdvertise",
			"container.deployment.dynamic",
			"container.deployment.dynamic.jarDirectory",
	};

	@Override
	protected void findUnknown(Properties properties)
			throws ConfigurationException {
		// backwards compatibility fixes
		for(int i=0;i<deprecated.length;i++){
			String name = deprecated[i];
			String v = properties.getProperty(name);
			if(v!=null){
				properties.remove(name);
				log.warn("Property "+name+" is DEPRECATED, ignoring.");
			}
		}
		super.findUnknown(properties);
	}

}