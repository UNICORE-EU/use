package eu.unicore.services.utils.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Feature;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;

/**
 * Base class for implementing Feature instances
 *
 * @author schuller
 */
public class FeatureImpl implements Feature {


	protected String name;

	protected Kernel kernel;

	private final List<Runnable> initTasks = new ArrayList<>();
	private final Map<String, Class<? extends Home>> homeClasses = new HashMap<>();
	private final List<DeploymentDescriptor> dd = new ArrayList<>();
	
	@Override
	public List<Runnable> getInitTasks(){
		return initTasks;
	}
	
	@Override
	public Map<String, Class<? extends Home>> getHomeClasses() {
		return homeClasses;
	}

	@Override
	public List<DeploymentDescriptor> getServices() {
		return dd;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}
	
	/**
	 * by default, the feature is enabled, and can be switched off 
	 * using a config property
	 * container.feature.NAME.enable=false 
	 */
	public boolean isEnabled(){
		Boolean enable = kernel.getContainerProperties().getBooleanValue("feature."+name+".enable");
		if(enable==null)enable=Boolean.TRUE;
		return enable;
	}
	
	/**
	 * by default, services that are part of the feature are enabled, but can be 
	 * switched off using a config property container.feature.service.NAME.enable=false  
	 */
	public boolean isServiceEnabled(String serviceName){
		Boolean enable = kernel.getContainerProperties().getBooleanValue("feature."+name+"."+serviceName+".enable");
		if(enable==null)enable=Boolean.TRUE;
		return enable;
	}
	
	public String toString(){
		return "Feature: "+name;
	}
}
