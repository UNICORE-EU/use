package de.fzj.unicore.wsrflite;


import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.exceptions.ServiceDeploymentException;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * 
 * @author j.daivandy@fzjuelich.de
 */
public class DeploymentManager {

	private static final Logger logger = Log.getLogger(Log.WSRFLITE,DeploymentManager.class);

	private final Kernel kernel;

	private final Map<String,Feature> features = new HashMap<>();
	
	public DeploymentManager(Kernel kernel) {
		this.kernel=kernel;
	}

	public synchronized void deployService(Service service)throws ServiceDeploymentException{
		if(kernel.getService(service.getName())!=null){
			logger.info("Service <"+service.getName()+"> already deployed, skipping.");
			return;
		}
		kernel.addService(service);
		try{
			service.start();
			if(service.getHome()!=null){
				kernel.putHome(service.getHome());
			}
			logger.info("Service <"+service.getName()+"> successfully deployed.");
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
			features.put(ftName, ft);
			enable = ft.isEnabled();
			if(enable){
				for(Map.Entry<String,Class<? extends Home>> h: ft.getHomeClasses().entrySet()){
					String name = h.getKey();
					Home home = kernel.getHome(name);
					if(home==null){
						home = h.getValue().getConstructor().newInstance();
						home.setKernel(kernel);
						home.activateHome(name);
						kernel.putHome(home);					}
				}
				for(DeploymentDescriptor dd: ft.getServices()){
					if(ft.isServiceEnabled(dd.getName())) {
						deployService(dd);
					}
					else {
						logger.info("Service <"+dd.getName()+"> is disabled, skipping.");
					}
				}
				for(Runnable r: ft.getInitTasks()){
					logger.info("Running <"+r.getClass().getName()+">");
					r.run();
				}
				logger.info("Feature <"+ftName+"> successfully deployed.");
			}
			else{
				logger.info("Feature <"+ftName+"> is disabled, skipping.");
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
	
}
