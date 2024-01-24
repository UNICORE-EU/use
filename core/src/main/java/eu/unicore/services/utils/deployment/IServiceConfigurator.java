package eu.unicore.services.utils.deployment;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Provides access to service configuration, including services definitions.
 * TODO - would be good to apply better separation of concerns here: implementations should
 * only return what was read from configuration (i.e. do only the parsing) and the 
 * dedicated code should actually deploy services. Would simplify dependency circles. 
 * @author K. Benedyczak
 */
public interface IServiceConfigurator {

	/**
	 * 
	 * @return properties loaded from the configuration
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public Properties loadProperties() throws IOException, ConfigurationException;
	
	/**
	 * Performs services configuration
	 * @throws Exception
	 */
	public void configureServices() throws Exception;
	
	/**
	 * return the list of {@link Runnable} that were defined in the configuration as service init tasks
	 */
	public List<Runnable>getInitTasks();

	public default void startConfigWatcher() {}
}

