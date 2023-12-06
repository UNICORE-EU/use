package eu.unicore.services;

import java.util.Collection;
import java.util.Collections;

/**
 * used to show the status of some subsystem
 * 
 * TODO add control, restart etc
 * 
 * @author schuller
 */
public interface ISubSystem {

	/**
	 * description for admins / logfiles
	 */
	public String getStatusDescription();

	/**
	 * the name of this subsystem
	 */
	public String getName();
	
	/**
	 * start this subsystem
	 */
	default public void start(Kernel kernel) throws Exception {}
	
	/**
	 * reload configuration
	 */
	default public void reloadConfig(Kernel kernel) throws Exception {}
	
	/**
	 * shutdown - will be called on kernel shutdown
	 */
	default public void shutdown(Kernel kernel) throws Exception {}
	
	default public Collection<ExternalSystemConnector> getExternalConnections(){
		return Collections.emptyList();
	}

}
