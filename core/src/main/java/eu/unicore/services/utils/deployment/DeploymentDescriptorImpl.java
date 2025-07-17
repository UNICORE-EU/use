package eu.unicore.services.utils.deployment;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Kernel;
import eu.unicore.services.StartupTask;

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

	protected final List<StartupTask>initTasks=new ArrayList<>();

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

	public List<StartupTask> getInitTasks() {
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
