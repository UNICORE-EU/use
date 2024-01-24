package eu.unicore.services;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.codahale.metrics.Metric;

/**
 * Control and check the status of some subsystem
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
	
	/**
	 * get the current list of {@link ExternalSystemConnector} instances
	 * for this subsystem (empty by default)
	 */
	default public Collection<ExternalSystemConnector> getExternalConnections(){
		return Collections.emptyList();
	}

	/**
	 * get the current map of {@link Metric} instances
	 * for this subsystem (empty by default)
	 */
	default public Map<String, Metric> getMetrics(){
		return Collections.emptyMap();
	}

}
