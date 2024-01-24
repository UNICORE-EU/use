package eu.unicore.services.security;

import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AttributeSourceConfigurator;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * IAttributeSourceBase provides the interface for UNICORE/X to retrieve authorisation information
 * (attributes) for a particular request from an attribute provider.
 * This is the base interface wich is extended by two actually used: {@link IAttributeSource} and 
 * {@link IDynamicAttributeSource} which are slightly different.
 * 
 * <em>Lifecycle</em>
 * IAttributeSourceBase implementations are created and initialised by the {@link ContainerSecurityProperties},
 * which will create the instance using Class.forName(), set additional parameters, and finally call
 * the init() method. The IAuthoriser will be created only once, and will be kept alive during the
 * lifetime of the server.
 * <p>
 * <em>Parameter injection</em>
 * When creating an IAttributeSource instance, UNICORE/X will set parameters according to the properties
 * defined in the main configuration file (usually <code>uas.config</code>), provided there is a public
 * setter method. For example, if the class has a field <code>setHost(String host)</code>, it
 * will be automatically invoked by UNICORE/X if the configuration has a property 
 * <code>
 * uas.security.attributes.NAME1.Host
 * </code>
 * Currently parameters can be of type String, boolean, or numerical, for details see {@link AttributeSourceConfigurator}
 * <p>
 * 
 * 
 * @author schuller
 * @author golbi
 */
public interface IAttributeSourceBase {

	/**
	 * Configures the source. After calling this method it must be ensured that 
	 * the attribute source is sane and woŕking.
	 * It is guaranteed that this method is invoked after all setter injections on the AIP.
	 * 
	 * @param name - the name of the attribute source
	 * @param kernel - the USE kernel
	 */
	public void configure(String name, Kernel kernel) throws ConfigurationException;

	/**
	 * should return the name this was configured with
	 */
	public default String getName() {
		return getClass().getSimpleName();
	};
}
