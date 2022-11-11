package eu.unicore.services;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.exceptions.ServiceDeploymentException;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * 
 * @author j.daivandy@fzjuelich.de
 */
public class DeploymentManager {

	private static final Logger logger = Log.getLogger(Log.UNICORE,DeploymentManager.class);

	private final Kernel kernel;

	private final Map<String,Feature> features = new HashMap<>();
	
	public DeploymentManager(Kernel kernel) {
		this.kernel=kernel;
	}

	public synchronized void deployService(Service service)throws ServiceDeploymentException{
		if(kernel.getService(service.getName())!=null){
			logger.info("Service <{}> already deployed, skipping.", service.getName());
			return;
		}
		kernel.addService(service);
		try{
			service.start();
			if(service.getHome()!=null){
				kernel.putHome(service.getHome());
			}
			logger.info("Service <{}> successfully deployed.", service.getName());
		}catch(Throwable ex){
			throw new ServiceDeploymentException("Can't deploy service <"+service.getName()+">",ex);
		}
	}

	public synchronized void deployService(DeploymentDescriptor info) throws ServiceDeploymentException {
		try {
			ServiceFactory sf=kernel.getServiceFactory(info.getType());
			Service service = sf.build(info);
			deployService(service);
		}
		catch(Exception e) {
			throw new ServiceDeploymentException(e);			
		}
	}

	public final Kernel getKernel(){
		return kernel;
	}

	public boolean deployFeature(Feature ft){
		Boolean enable = Boolean.FALSE;
		try{
			String ftName = ft.getName();
			String ftVersion = ft.getVersion();
			features.put(ftName, ft);
			enable = ft.isEnabled();
			if(enable){
				for(Map.Entry<String,Class<? extends Home>> h: ft.getHomeClasses().entrySet()){
					String name = h.getKey();
					Home home = kernel.getHome(name);
					if(home==null){
						home = h.getValue().getConstructor().newInstance();
						home.setKernel(kernel);
						home.start(name);
						kernel.putHome(home);					}
				}
				for(DeploymentDescriptor dd: ft.getServices()){
					if(ft.isServiceEnabled(dd.getName())) {
						deployService(dd);
					}
					else {
						logger.info("Service <{}> is disabled, skipping.", dd.getName());
					}
				}
				ft.initialise();
				logger.info("Feature {} v{} successfully deployed.", ftName, ftVersion);
			}
			else{
				logger.info("Feature <{}> disabled, skipping.", ftName);
			}
		}catch(Exception ex){
			String msg = Log.createFaultMessage("Cannot deploy feature <"+ft.getName()+">", ex);
			throw new ConfigurationException(msg,ex);
		}
		return enable;
	}
	
	public boolean isFeatureEnabled(String name){
		Feature f = features.get(name);
		return f!=null && f.isEnabled();
	}
	
	public Map<String, Feature> getFeatures(){
		return Collections.unmodifiableMap(features);
	}
}
