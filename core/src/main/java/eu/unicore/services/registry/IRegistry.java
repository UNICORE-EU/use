package eu.unicore.services.registry;

import java.util.List;
import java.util.Map;

/**
 * basic operations common to local/external registries
 * 
 * @author schuller
 */
public interface IRegistry {

	public List<Map<String,String>> listEntries() throws Exception;
	
}
