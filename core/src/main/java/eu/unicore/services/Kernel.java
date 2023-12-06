/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package eu.unicore.services;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.apache.logging.log4j.Logger;

import com.codahale.metrics.MetricRegistry;

import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.security.wsutil.SecuritySessionStore;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionLoader;
import eu.unicore.services.messaging.IMessaging;
import eu.unicore.services.messaging.MessagingException;
import eu.unicore.services.messaging.MessagingImpl;
import eu.unicore.services.persistence.PersistenceManager;
import eu.unicore.services.registry.RegistryCreator;
import eu.unicore.services.security.CertificateInfoMetric;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.IDynamicAttributeSource;
import eu.unicore.services.security.SecurityManager;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.services.server.ContainerHttpServerProperties;
import eu.unicore.services.server.GatewayHandler;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.server.StartupTasksRunner;
import eu.unicore.services.utils.CapabilitiesLoader;
import eu.unicore.services.utils.deployment.IServiceConfigurator;
import eu.unicore.services.utils.deployment.NullServiceConfigurator;
import eu.unicore.services.utils.deployment.PropertyChecker;
import eu.unicore.services.utils.deployment.ServiceConfigurator;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.UpdateableConfiguration;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Central class of USE, serves for lookup of components, services, and
 * properties
 * 
 * @author schuller
 */
public final class Kernel {

	private static final Logger logger = Log.getLogger(Log.UNICORE, Kernel.class);

	private IMessaging msg;

	private JettyServer jetty;

	private SecurityManager securityManager;

	private PersistenceManager persistenceManager;

	private final MetricRegistry metricRegistry = new MetricRegistry();
	
	private final Map<String, Home> homes = Collections
			.synchronizedMap(new HashMap<>());

	private final Map<String, Service> services = Collections
			.synchronizedMap(new HashMap<>());

	private final Map<String, ServiceFactory> serviceFactories = Collections
			.synchronizedMap(new HashMap<>());

	private GatewayHandler gwHandler;

	private final List<PropertyChecker> propertyCheckers = new ArrayList<>();
	private ContainerProperties containerConfiguration;
	private ContainerSecurityProperties containerSecurityConfiguration;
	private USEClientProperties clientConfiguration;
	private HttpServerProperties jettyConfiguration;
	private PersistenceProperties persistenceProperties;

	private IServiceConfigurator serviceConfigurator;

	private Map<String,AdminAction> adminActions;

	private Map<String,Capability> capabilities;

	private final Collection<UpdateableConfiguration> configurations
		= new HashSet<>();

	private DeploymentManager deploymentManager;

	private volatile boolean isShutdown=false;

	private Calendar upSince;

	private final Map<Class<?>,Object>attributes = new HashMap<>();

	private final Collection<ISubSystem> subSystems = new HashSet<>();

	/**
	 * Creates Kernel instance using only a loaded Properties as configuration
	 * and initializes its internal state (co-working objects, configuration). 
	 * For instance useful for unit testing.
	 * @param extraProperties see description
	 * @throws Exception 
	 */
	public Kernel(Properties extraProperties) throws Exception {
		this(null, extraProperties);
	}

	/**
	 * Creates Kernel using a main configuration file path.
	 * Kernel is initialized (co-working objects, configuration). 
	 * @param configurationFile see description
	 * @throws Exception 
	 */
	public Kernel(String configurationFile) throws Exception {
		this(configurationFile, null);
	}

	/**
	 * Creates Kernel using a main configuration file path. This file must be a Java properties.
	 * Additional settings can be provided with the 2nd argument. Those properties will be added (overwriting 
	 * if necessary) to the properties read from the configuration file.
	 * Kernel is initialized (co-working objects, configuration). 
	 * @param configurationFile see description
	 * @param extraProperties see description
	 * @throws Exception 
	 */
	public Kernel(String configurationFile, Properties extraProperties) throws Exception {
		super();
		logger.info(getHeader());
		upSince=Calendar.getInstance();
		registerDefaultFactories();
		prepare(configurationFile, extraProperties);
	}

	public static final String getVersion() {
		return getVersion(Kernel.class);
	}
	

	public static String getVersion(Class<?> versionOf) {
		String version = versionOf.getPackage().getSpecificationVersion(); 
		return version != null ? version : "DEVELOPMENT";
	}

	public final String getHeader() {
		String lineSep = System.getProperty("line.separator");
		String s = lineSep
				+ " _    _ _   _ _____ _____ ____  _____  ______" + lineSep
				+ "| |  | | \\ | |_   _/ ____/ __ \\|  __ \\|  ____|" + lineSep
				+ "| |  | |  \\| | | || |   | |  | | |__) | |__"+ lineSep
				+ "| |  | | . ` | | || |   | |  | |  _  /|  __|"+ lineSep
				+ "| |__| | |\\  |_| |_ |____ |__| | | \\ \\| |____"+ lineSep
				+ " \\____/|_| \\_|_____\\_____\\____/|_|  \\_\\______|"+ lineSep 
				+ "UNICORE Services Environment (v"+getVersion()+"), "
				+ "https://www.unicore.eu"+ lineSep;
		return s;
	}

	public String getConnectionStatus(){
		StringBuilder report = new StringBuilder();
		String newline = System.getProperty("line.separator");
		List<ExternalSystemConnector> ext = new ArrayList<>();
		report.append("Subsystems");
		report.append(newline);
		report.append("***********");
		report.append(newline);
		if(subSystems.size()==0) {
			report.append("N/A").append(newline);
		}
		for(ISubSystem s: getSubSystems()) {
			ext.addAll(s.getExternalConnections());
			try{
				report.append(s.getName())
				.append(": ")
				.append(s.getStatusDescription())
				.append(newline);
			}catch(Exception ex){
				return "ERROR: " + ex.toString();
			}
		}
		report.append(newline);
		report.append("External connections");
		report.append(newline);
		report.append("********************");
		report.append(newline);
		for(ExternalSystemConnector esc: ext){
			report.append(esc.getExternalSystemName());
			report.append(": ");
			report.append(esc.getConnectionStatusMessage());
			report.append(newline);
		}
		report.append(newline);
		return report.toString();
	}


	public Calendar getUpSince(){
		return upSince;
	}
	/**
	 * shutdown all services in a clean manner
	 */
	public synchronized void shutdown() {
		if(isShutdown)return;
		isShutdown=true;
		state = State.shutting_down;
		
		try {
			if (getServer() != null) {
				getServer().stop();
			}
		} catch (Exception e) {
			Log.logException("Problem shutting down the Jetty server", e,
					logger);
		}
		for (Service s : services.values()) {
			try {
				s.stop();
			} catch (Throwable t) {
				logger.error("Error during shutdown of service <{}>", s.getName(), t);
			}
		}
		for (Home h : homes.values()) {
			try {
				h.shutdown();
			} catch (Throwable t) {
				logger.error("Error during shutdown of resource <{}>", h.getServiceName(), t);
			}
		}
		for (ISubSystem sub : subSystems) {
			try {
				sub.shutdown(this);
			} catch (Throwable t) {
				logger.error("Error during shutdown of resource <{}>", sub.getName(), t);
			}
		}
	}

	public JettyServer getServer() {
		return jetty;
	}

	public void addPropertyChecker(PropertyChecker checker){
		this.propertyCheckers.add(checker);
	}

	/**
	 * register a service
	 * 
	 * @param service - the service to register
	 * @return the service of the same name that was previously registered, or null
	 */
	public Service addService(Service service) {
		return services.put(service.getName(), service);
	}

	/**
	 * deregister a service
	 * @param serviceName
	 */
	public Service removeService(String serviceName) {
		return services.remove(serviceName);
	}

	/**
	 * returns a typed kernel attribute 
	 * @param key - the attribute class used as key
	 * @return the attribute
	 */
	public <T> T getAttribute(Class<T>key){
		Object o=attributes.get(key);
		return  key.cast(o);
	}

	/**
	 * store a typed kernel attribute
	 * @param key - the value's class as key
	 * @param value - the value
	 */
	public <T> void setAttribute(Class<T>key, T value){
		logger.debug("Storing attribute: {}", key.getName());
		attributes.put(key, value);
	}

	public void register(Object candidate) {
		if(candidate!=null && candidate instanceof ISubSystem) {
			if(!subSystems.contains(candidate)) {
				subSystems.add((ISubSystem)candidate);
			}
		}
	}
	
	public void unregister(Class<?> type) {
		Iterator<ISubSystem> i = subSystems.iterator();
		while(i.hasNext()) {
			ISubSystem sub = i.next();
			if(sub.getClass().isAssignableFrom(type)) {
				i.remove();
			}
		}
	}

	/**
	 * store a configuration object which will receive
	 * updates when change of the Kernel's configuration file is detected.
	 * @param config
	 */
	public void addConfigurationHandler(UpdateableConfiguration config){
		configurations.add(config);
	}

	/**
	 * get the service of the given name
	 * 
	 * @param name
	 */
	public Service getService(String name) {
		return services.get(name);
	}

	public ServiceFactory getServiceFactory(String type) {
		return serviceFactories.get(type);
	}


	/**
	 * get a read-only list of all service factories
	 */
	public Collection<ServiceFactory> getServiceFactories() {
		return Collections.unmodifiableCollection(serviceFactories.values());
	}

	/**
	 * get a read-only list of all services
	 */
	public Collection<Service> getServices() {
		return Collections.unmodifiableCollection(services.values());
	}

	/**
	 * retrieve the named home
	 * 
	 * @param serviceName
	 *            - the name of the service
	 */
	public Home getHome(String serviceName) {
		Home h = homes.get(serviceName);
		if(h==null){
			Service s = services.get(serviceName);
			if(s != null)h=s.getHome();
		}
		return h;
	}

	public void putHome(Home home){
		homes.put(home.getServiceName(), home);
	}

	/**
	 * Retrieve the container security configuration
	 */
	public ContainerSecurityProperties getContainerSecurityConfiguration() {
		return containerSecurityConfiguration;
	}

	/**
	 * @return configuration for the client calls made by the container
	 */
	public USEClientProperties getClientConfiguration() {
		return (USEClientProperties)clientConfiguration.clone();
	}

	public ContainerProperties getContainerProperties() {
		return containerConfiguration;
	}

	public HttpServerProperties getJettyProperties() {
		return jettyConfiguration;
	}

	public PersistenceProperties getPersistenceProperties() {
		return persistenceProperties;
	}

	/**
	 * get access to the message system
	 */
	public IMessaging getMessaging() throws MessagingException {
		return msg;
	}

	/**
	 * get the security manager
	 */
	public SecurityManager getSecurityManager() {
		return securityManager;
	}
	
	/**
	 * retrieve the server-wide security session store, or create it if not already done so
	 */
	public synchronized SecuritySessionStore getOrCreateSecuritySessionStore(){
		SecuritySessionStore securitySessionStore = getAttribute(SecuritySessionStore.class);
		if(securitySessionStore == null){
			int sessionsPerUser = getContainerSecurityConfiguration().getMaxSessionsPerUser();
			securitySessionStore = new SecuritySessionStore(sessionsPerUser);
			setAttribute(SecuritySessionStore.class, securitySessionStore);
		}
		return securitySessionStore;
	}
	
	/**
	 * get the persistence manager
	 */
	public PersistenceManager getPersistenceManager() {
		return persistenceManager;
	}

	public MetricRegistry getMetricRegistry() {
		return metricRegistry;
	}

	public final GatewayHandler getGatewayHandler(){
		return gwHandler;
	}

	public DeploymentManager getDeploymentManager(){
		return deploymentManager;
	}

	/**
	 * gets the map of available admin actions
	 * @return a non-null, unmodifiable map
	 */
	public Map<String,AdminAction> getAdminActions(){
		return adminActions;
	}

	public Collection<ISubSystem>getSubSystems(){
		return subSystems;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Collection<T>getCapabilities(Class<T> type){
		List<T>result = new ArrayList<>();
		for(Capability c: capabilities.values()){
			if(type.isAssignableFrom(c.getClass())){
				result.add((T)c);
			}
		}
		return result;
	}

	/**
	 * update things after containerSecurityConfiguration has reloaded the main credential
	 */
	public void credentialReloaded() {
		clientConfiguration.setCredential(containerSecurityConfiguration.getCredential());
		PubkeyCache.get(this).update(containerSecurityConfiguration.getCredential());
		RegistryCreator rc = RegistryCreator.get(this);
		if(rc.haveRegistry() && !rc.isGlobalRegistry()) {
			rc.refreshRegistryEntries();
		}
	}

	/**
	 * Initializes configuration objects from the data stored in Kernel's properties. Called only once
	 * indirectly from constructor.
	 */
	private void initializeConfiguration(Properties properties) throws ConfigurationException {
		for (PropertyChecker checker: propertyCheckers) {
			checker.checkProperties(properties, logger);
		}
		containerSecurityConfiguration = new ContainerSecurityProperties(properties);
		containerConfiguration = new ContainerProperties(properties, 
				containerSecurityConfiguration.isSslEnabled());
		clientConfiguration = new USEClientProperties(properties, containerSecurityConfiguration);

		if(state==State.configuring) {
			// these are done only at server startup
			jettyConfiguration = new ContainerHttpServerProperties(properties);
			PersistenceFactory pf = PersistenceFactory.get(new PersistenceProperties(properties));
			persistenceProperties = pf.getConfig();
			try {
				String pdpConfig = containerSecurityConfiguration.getPdpConfigurationFile();
				if (pdpConfig != null)
					logger.info("Using PDP configuration file <{}>", pdpConfig);
				containerSecurityConfiguration.getPdp().initialize(pdpConfig, containerConfiguration, 
						containerSecurityConfiguration, clientConfiguration);
			} catch (Exception e) {
				throw new ConfigurationException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Creates instances of all co-working classes.
	 * @throws Exception 
	 */
	private void initializeBuddies() throws Exception {
		msg = new MessagingImpl(getPersistenceProperties());
		persistenceManager=new PersistenceManager(this);
		deploymentManager=new DeploymentManager(this);
		jetty = new JettyServer(this, jettyConfiguration);
		securityManager = new SecurityManager(containerSecurityConfiguration);
		metricRegistry.register("use.security.ServerIdentity",new CertificateInfoMetric(securityManager));
		adminActions = AdminActionLoader.load();
		gwHandler = new GatewayHandler(getContainerProperties(), getClientConfiguration(), 
				containerSecurityConfiguration);
		subSystems.add(gwHandler);
		capabilities = CapabilitiesLoader.load(this);
		IAttributeSource aip = containerSecurityConfiguration.getAip();
		aip.start(this);
		register(aip);
		IDynamicAttributeSource dap = containerSecurityConfiguration.getDap();
		dap.start(this);
		register(dap);
	}

	public void refreshConfig() throws Exception {
		logger.info("Reloading configuration");
		Properties newProperties = serviceConfigurator.loadProperties();
		initializeConfiguration(newProperties);
		for(UpdateableConfiguration uc: configurations) {
			uc.setProperties(newProperties);
		}
		for(ISubSystem sub: subSystems) {
			sub.reloadConfig(this);
		}
		logger.info(getConnectionStatus());
	}

	/**
	 * The main initialization logic.
	 * Prepare for the start: loads configuration, setup cooperating objects but does not start
	 * any services yet.
	 * 
	 * @param conf -config file path or null if not available
	 * @throws Exception
	 */
	private void prepare(String conf, Properties extraSettings) throws Exception {
		if (conf != null) {
			File file = new File(conf);
			serviceConfigurator = new ServiceConfigurator(this, file);
			
		} else {
			serviceConfigurator = new NullServiceConfigurator();
		}

		addPropertyChecker(new USEPropertyChecker());

		Properties p = serviceConfigurator.loadProperties();
		if (extraSettings != null)
			p.putAll(extraSettings);

		initializeConfiguration(p);

		initializeBuddies();

		initGateway();

	}

	/**
	 * Low level method starting the server: it doesn't print any banners etc., only performs
	 * the actual startup logic.
	 * 
	 * @throws Exception
	 */
	public void start()throws Exception{
		isShutdown = false;
		state = State.initializing;
		jetty.start();
		var startupTasks = deployServices();
		var sl = ServiceLoader.load(StartupTask.class);
		StartupTasksRunner.runStartupTasks(this, sl);
		homes.values().forEach( h -> h.run() );
		state = State.running;
		// run remaining startup tasks after basic setup is complete
		startupTasks.forEach( task -> {
			logger.info("Running startup task <{}>", task.getClass().getName());
			task.run();
		});
		addShutDownHook();
		serviceConfigurator.startConfigWatcher();
	}

	/**
	 * starts the container asynchronously, i.e. this method returns
	 * immediately
	 * 
	 * @see #startSynchronous()
	 */
	public void startAsync() throws Exception {
		Thread t = new Thread( ()-> {
			try {
				startSynchronous();
			} catch (Throwable e) {
				Log.logException("Error during server start.", e, logger);
				System.err.println("ERROR DURING SERVER STARTUP!");
				e.printStackTrace();
				System.exit(1);
			}
		});
		t.setName("UNICORE-Startup");
		t.start();
	}

	/**
	 * starts the container synchronously: starts the server and 
	 * return only after server is started.
	 * 
	 * @throws Exception
	 */
	public void startSynchronous() throws Exception {
		long start = System.currentTimeMillis();
		printHeader();				
		start();
		logger.info("Startup time: {} ms.", System.currentTimeMillis() - start);
		logger.info("***** Server started. *****");
		System.out.println("***** Server started. *****");
		System.out.println("Send TERM signal to shutdown gracefully.");
	}

	public void printHeader() {
		System.out.println(getHeader());
	}


	// lookup and register service factories from classpath
	private void registerDefaultFactories() {
		var sl = ServiceLoader.load(ServiceFactory.class);
		sl.forEach( factory -> {
			serviceFactories.put(factory.getType(), factory);
			logger.info("Registered '{}' for service type '{}'", factory.getClass().getName(), factory.getType());
		});
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread( ()-> {
			try {
				shutdown();
			} catch (Exception e) {
				Log.logException("Error during shutdown", e, logger);
			}
		}, "UNICORE-Shutdown"));
	}

	/**
	 * deploy and configure services
	 * 
	 * @throws Exception
	 */
	private List<Runnable> deployServices() throws Exception {
		serviceConfigurator.configureServices();
		return serviceConfigurator.getInitTasks();
	}

	private void initGateway()throws Exception{
		//will wait if needed for gateway. Also will update the gateway's DN if needed.
		gwHandler.waitForGateway();
		// if appropriate, dynamically register with gateway
		gwHandler.enableGatewayRegistration();
		// if appropriate, check & update GW certificate periodically
		gwHandler.enableGatewayCertificateRefresh();
	}

	/**
	 * load an object instance and inject the Kernel instance if required
	 * @param clazz
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public <T> T load(Class<T> clazz)throws NoSuchMethodException, 
	InvocationTargetException, IllegalAccessException, InstantiationException{
		T o=null;
		Constructor<T>con = null;
		for(Constructor<?>c: clazz.getConstructors()){
			Class<?>[]params=c.getParameterTypes();
			if(params!=null && params.length==1 && params[0].isAssignableFrom(Kernel.class)){
				con = clazz.getConstructor(Kernel.class);		
			}
		}

		if(con!=null)
			o = con.newInstance(this);
		else{
			o = clazz.getConstructor().newInstance();
			if(o instanceof KernelInjectable)
				((KernelInjectable)o).setKernel(this);
		}

		return o;
	}
	
	public static enum State {
		configuring, initializing, running, shutting_down,
	}
	
	private volatile State state = State.configuring;
	
	public State getState(){
		return state;
	}
	
	/**
	 * returns true if the container is operational and ready
	 * to process external requests
	 */
	public boolean isAvailable(){
		return State.running == state;
	}

}
