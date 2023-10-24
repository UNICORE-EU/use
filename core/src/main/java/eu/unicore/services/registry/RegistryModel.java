package eu.unicore.services.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.services.impl.BaseModel;

public class RegistryModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	// maps endpoints to ServiceRegistryEntry UUIDs
	private Map<String, String> entries = new HashMap<>();
	
	public void put(String endpoint, String uuid){
		entries.put(endpoint, uuid);
	}
	
	public String getEntryID(String endpoint){
		return entries.get(endpoint);
	}

	public void removeEntry(String endpoint){
		entries.remove(endpoint);
	}

	public Collection<String> getEntryIDs(){
		return Collections.unmodifiableCollection(entries.values());
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
