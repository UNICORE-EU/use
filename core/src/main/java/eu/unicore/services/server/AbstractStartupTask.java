package eu.unicore.services.server;

import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.StartupTask;

/**
 * Base implementation of startup task: name is the class name, after and before are not restricted. 
 * @author K. Benedyczak
 */
public abstract class AbstractStartupTask implements StartupTask, KernelInjectable {

	private Kernel kernel;

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public Kernel getKernel() {
		return this.kernel;
	}
}
