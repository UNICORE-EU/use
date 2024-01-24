package eu.unicore.services.security;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.OperationType;

/**
 * Allows to establish during initialization what are the types of operations on a SEI
 * and to return this information later on.
 * <p>
 * Parts of the code are based on Spring AnnotationUtils - thanks.
 * 
 * @author K. Benedyczak
 */
public class OperationTypesUtil {

	private final Map<String, Map<String, OperationType>> types = new HashMap<>();
	
	public void addService(String serviceName, Map<String, OperationType> types) {
		this.types.put(serviceName, types);
	}
	
	
	/**
	 * @param serviceName 
	 * @param operation
	 * @return type of operation. If no type is defined with annotation, then by default "modify" is returned.
	 */
	public OperationType getOperationType(String serviceName, String operation)
	{
		if (operation == null || serviceName == null)
			return OperationType.modify;
		
		Map<String, OperationType> classTypes = types.get(serviceName);
		if (classTypes == null)
			return OperationType.modify;
		OperationType ret = classTypes.get(operation);
		if (ret == null)
			return OperationType.modify;
		return ret;
	}
}
