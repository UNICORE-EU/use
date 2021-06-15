package eu.unicore.services.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.services.impl.BaseModel;

public class ServiceRegistryModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	// maps endpoints to ServiceRegistryEntry UUIDs
	private final Map<String, String> entries = new HashMap<>();
	
	// maps endpoints to content
	private final Map<String, Map<String,String>> contents = new HashMap<>();
		
	public void put(String endpoint, String uuid, Map<String,String>content){
		entries.put(endpoint, uuid);
		contents.put(endpoint, content);
	}
	
	public String getEntryID(String endpoint){
		return entries.get(endpoint);
	}
	
	public Map<String,String>getContent(String endpoint){
		return contents.get(endpoint);
	}
	
	public void removeEntry(String endpoint){
		entries.remove(endpoint);
		contents.remove(endpoint);
	}
	
	public Map<String,Map<String,String>>getContents(){
		return Collections.unmodifiableMap(contents);
	}
	
	@Override
	public String getFrontend(String serviceType) {
		return "de.fzj.unicore.wsrflite.registry.ws.SGFrontend";
	}
	
	@Override
	public boolean removeChild(String uid){
		String endpoint = null;
		for(Map.Entry<String,String> entry: entries.entrySet()){
			if(entry.getValue().equals(uid)){
				endpoint = entry.getKey();
				break;
			}
		}
		if(endpoint!=null)removeEntry(endpoint);
		return super.removeChild(uid);
	}
}
