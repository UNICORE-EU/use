package de.fzj.unicore.wsrflite.admin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fzj.unicore.wsrflite.Kernel;

/**
 * Allows to toggle that availability of certain ws-resources.
 * If set to unavailable, all attempts to access will result in a 
 * "Resource unavailable" fault. 
 * The state will be cleared by a server restart.
 * 
 * @author schuller
 */
public class ResourceAvailability implements AdminAction {
	
	@Override
	public String getName() {
		return "ToggleResourceAvailability";
	}

	@Override
	public String getDescription() {
		return "'resources' - comma separated list of IDs";
	}
	
	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		StringBuilder msg=new StringBuilder();
		String idKey=params.get("resources");
		if(idKey!=null){
			String[] ids=idKey.split(",");
			for(String id: ids){
				boolean available=resources.remove(id);
				if(!available){
					resources.add(id.trim());
				}
				msg.append(id).append("=").append(available?"available ":"unavailable ");
			}
		}
		AdminActionResult res=new AdminActionResult(true, msg.toString());
		return res;
	}

	private static final Set<String>resources=Collections.synchronizedSet(new HashSet<String>());
	
	public static boolean isUnavailable(String resourceID){
		return resources.contains(resourceID);
	}

}
