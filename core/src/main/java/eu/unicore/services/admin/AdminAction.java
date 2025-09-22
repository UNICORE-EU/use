package eu.unicore.services.admin;

import java.util.Map;

import eu.unicore.services.Kernel;

/**
 * An AdministrativeAction that is used to perform some task in the container. 
 * It has a name, since it may be possible to invoke this action via some remote operation.
 * Additionally a human readable description is provided.
 *
 * @author schuller
 */
public interface AdminAction {

	/**
	 * gets the name of this action
	 */
	public String getName();

	/**
	 * gets the single line summary (including at least the accepted parameter names) 
	 * of this admin action
	 */
	public String getDescription();

	/**
	 * invoke this action
	 * 
	 * @param params - the parameters
	 * @param kernel - the {@link Kernel}
	 * @return an {@link AdminActionResult}
	 */
	public AdminActionResult invoke(Map<String,String>params, Kernel kernel);

}
