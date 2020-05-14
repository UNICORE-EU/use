package de.fzj.unicore.wsrflite.utils.deployment;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;

/**
 * Base class for implementing DeploymentDescriptor instances
 *
 * @author schuller
 */
public class DeploymentDescriptorImpl implements DeploymentDescriptor {
	
	protected String name;
	
	protected String type;
	
	protected Class<?> interfaceClass;
	
	protected Class<?> implementationClass;
	
	protected Class<?> frontendClass;
	
	protected final List<String>inHandlers=new ArrayList<>();
	
	protected final List<String>outHandlers=new ArrayList<>();

	protected final List<Runnable>initTasks=new ArrayList<>();

	protected Kernel kernel;

	protected boolean enabled = true;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Class<?> getInterface() {
		return interfaceClass;
	}

	public void setInterface(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public Class<?> getImplementation() {
		return implementationClass;
	}

	public void setImplementation(Class<?> implementationClass) {
		this.implementationClass = implementationClass;
	}

	public Class<?> getFrontend() {
		return frontendClass;
	}

	public void setFrontend(Class<?> frontendClass) {
		this.frontendClass = frontendClass;
	}

	public List<String> getInHandlers() {
		return inHandlers;
	}

	public void setInHandlers(List<String> inHandlers) {
		this.inHandlers.clear();
		this.inHandlers.addAll(inHandlers);
	}

	public List<String> getOutHandlers() {
		return outHandlers;
	}

	public void setOutHandlers(List<String> outHandlers) {
		this.outHandlers.clear();
		this.outHandlers.addAll(outHandlers);
	}

	public List<Runnable> getInitTasks() {
		return initTasks;
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}
	
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public String toString(){
		return name+"[type:"+type+" impl:"+implementationClass
				+" iface:"+interfaceClass+"]";
	}
}
