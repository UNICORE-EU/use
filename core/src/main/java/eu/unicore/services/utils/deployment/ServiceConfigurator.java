package eu.unicore.services.utils.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Feature;
import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.ServiceFactory;
import eu.unicore.services.exceptions.ServiceDeploymentException;
import eu.unicore.services.utils.FileWatcher;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Read services deployment info from properties and deploy services
 * 
 * @author schuller
 */
public class ServiceConfigurator implements IServiceConfigurator {

	private static final Logger logger=Log.getLogger(Log.UNICORE,ServiceConfigurator.class);
	
	private Properties properties;
	
	private final File configFile;
	
	private final Kernel kernel;
	
	private final List<Runnable> startupTasks = new ArrayList<>();
	
	private FileWatcher configFileWatcher;
	
	/**
	 * Configure from the given file
	 *
	 * @param kernel
	 * @param config - the config file
	 * 
	 * @throws FileNotFoundException
	 */
	public ServiceConfigurator(Kernel kernel, File config) throws FileNotFoundException {
		this.kernel=kernel;
		this.configFile=config;
	}
	
	public Properties loadProperties() throws IOException, ConfigurationException {
		properties=new Properties();
		try(FileInputStream fis=new FileInputStream(configFile)){
			properties.load(fis);
			logger.info("Loaded properties from file <{}>", configFile);
		}
		return properties;
	}
	
	public void configureServices() throws Exception{
		if (properties == null)
			throw new IllegalStateException("loadProperties() must be invoked prior to this method");
		doConfigureFeatures();
		doConfigureServices();
		addUserDefinedStartupTasks();
	}
	
	private void doConfigureFeatures() throws Exception {
		ServiceLoader<Feature> fl = ServiceLoader.load(Feature.class);
		for (Feature ft: fl) {
			ft.setKernel(kernel);
			deployFeature(ft);
		}
	}
	
	private void doConfigureServices() throws Exception {
		ServiceLoader<DeploymentDescriptor> sl = ServiceLoader.load(DeploymentDescriptor.class);
		for (DeploymentDescriptor dd: sl) {
			dd.setKernel(kernel);
			deployService(dd);
		}
	}
	
	/*
	 * add init tasks defined in a property "container.onstartup"
	 */
	private void addUserDefinedStartupTasks()throws Exception{
		ContainerProperties settings = kernel.getContainerProperties();
		List<String> values = settings.getListOfValues(ContainerProperties.ON_STARTUP_LIST_KEY);
		if (settings.isSet(ContainerProperties.ON_STARTUP_KEY))
			values.add(0, settings.getValue(ContainerProperties.ON_STARTUP_KEY));
		
		for(String value: values) {
			if (value!=null) {
				String[] classes = value.split(" ");
				for (String c:classes) {
					try {
						c=c.trim();
						if (c.length()==0)
							continue;
						Class<?>clazz = Class.forName(c);
						Object o=kernel.load(clazz);
						logger.info("Loaded startup task <{}>", c);
						startupTasks.add((Runnable)o);
					}catch(Exception e){
						logger.warn("Error loading startup task <{}>: {}", c, Log.createFaultMessage("", e));
					}
				}
			}
		}
	}

	protected void deployFeature(Feature ft){
		if(kernel.getDeploymentManager().deployFeature(ft)) {
			startupTasks.addAll(ft.getStartupTasks());
		}
	}
	
	protected void deployService(DeploymentDescriptor dd){
		boolean deploy = true;
		try{
			if(dd.isEnabled()){
				//only deploy if service is not already deployed
				if(kernel.getService(dd.getName())==null){
					doDeployService(dd);
				}
			}
			else{
				deploy = false;
				logger.info("Disabling service <{}>", dd.getName());
				//check if service already is deployed and undeploy it
				Service s=kernel.getService(dd.getName());
				if(s!=null){
					s.stop();
				}
			}
		}catch(Exception ex){
			String msg="Cannot "+(deploy?"deploy":"undeploy")+" service <"+dd.getName()+">";
			Log.logException(msg, ex, logger);
		}
	}
	
	private void doDeployService(DeploymentDescriptor dd) throws Exception {
		ServiceFactory f=kernel.getServiceFactory(dd.getType());
		if(f==null){
			throw new ServiceDeploymentException("No factory available for service type <"+dd.getType()+">");
		}
		kernel.getDeploymentManager().deployService(f.build(dd));
		startupTasks.addAll(dd.getInitTasks());
	}

	/**
	 * return the list of {@link Runnable} that were defined in the 
	 * configuration as service init tasks
	 */
	public List<Runnable>getInitTasks(){
		return startupTasks;
	}

	public void startConfigWatcher() {
		try {
			configFileWatcher = new FileWatcher(configFile, ()->{
				try {
					kernel.refreshConfig();
				}catch(Exception ex) {
					logger.error("Error refreshing configuration.", ex);
				}
			});
		}catch(FileNotFoundException ex) {
			throw new ConfigurationException("Error setting up config file watcher", ex);
		}
		kernel.getContainerProperties().getThreadingServices().getScheduledExecutorService()
			.scheduleWithFixedDelay(configFileWatcher, 10, 10, TimeUnit.SECONDS);
		logger.info("Started monitoring of {} with interval {}s", configFile, 10);
	}
}

