package eu.unicore.services.server;

import java.util.Collection;
import java.util.Collections;

import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.StartupTask;

/**
 * Base implementation of startup task: name is the class name, after and before are not restricted. 
 * @author K. Benedyczak
 */
public abstract class AbstractStartupTask implements StartupTask, KernelInjectable {
	private Kernel kernel;
	
	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public Collection<String> getAfter() {
		return Collections.emptySet();
	}

	@Override
	public Collection<String> getBefore() {
		return Collections.emptySet();
	}
	
	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public Kernel getKernel() {
		return this.kernel;
	}
}
