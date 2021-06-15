package eu.unicore.services.utils.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Feature;
import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.ServiceFactory;
import eu.unicore.services.exceptions.ServiceDeploymentException;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Read services deployment info from properties and deploy services
 * 
 * @author schuller
 */
public class ServiceConfigReader implements IServiceConfigurator {

	private static final Logger logger=Log.getLogger(Log.UNICORE,ServiceConfigReader.class);
	
	private Properties properties;
	
	private final File configFile;
	
	private final Kernel kernel;
	
	private final List<Runnable> initTasks = new ArrayList<>();
	
	/**
	 * Configure from the given file. This file will be watched for changes using 
	 * the given period (use a negative number to disable the watch), after the startConfigMonitoring()
	 * method is invoked.
	 * 
	 * @param kernel
	 * @param config - the config file
	 * 
	 * @throws FileNotFoundException
	 */
	public ServiceConfigReader(Kernel kernel, File config) throws FileNotFoundException {
		this.kernel=kernel;
		this.configFile=config;
	}
	
	public Properties loadProperties() throws IOException, XMLStreamException, ConfigurationException {
		properties=new Properties();
		try(FileInputStream fis=new FileInputStream(configFile)){
			properties.load(fis);
			logger.info("Loaded properties from file <"+configFile+">");
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
						logger.info("Found startup task <"+c+">");
						Class<?>clazz = Class.forName(c);
						Object o=kernel.load(clazz);
						initTasks.add((Runnable)o);
					}catch(Exception e){
						Log.logException("Error setting up startup task " + c, e, logger);
					}
				}
			}
		}
	}

	protected void deployFeature(Feature ft){
		kernel.getDeploymentManager().deployFeature(ft);
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
				logger.info("Disabling service <"+dd.getName()+">");
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
		initTasks.addAll(dd.getInitTasks());
	}

	/**
	 * return the list of {@link Runnable} that were defined in the 
	 * configuration as service init tasks
	 */
	public List<Runnable>getInitTasks(){
		return initTasks;
	}

	/**
	 * return the time the config file was last modified
	 * @return the config file's {@link File#lastModified()} or -1 if no config file was defined
	 */
	public long getLastConfigFileUpdateTime(){
		return configFile!=null? configFile.lastModified() : -1; 
	}
	
}

