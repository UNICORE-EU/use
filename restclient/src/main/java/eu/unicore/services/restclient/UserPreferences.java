package eu.unicore.services.restclient;

import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.HttpMessage;

public class UserPreferences {

	private final Map<String,String> prefs = new HashMap<>();

	public void addUserPreferencesHeader(HttpMessage httpMessage) throws Exception {
		if(prefs.size()>0){
			httpMessage.addHeader("X-UNICORE-User-Preferences", getEncoded());
		}
	}

	public String getEncoded(){
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<String, String> e: prefs.entrySet()){
			if(sb.length()>0)sb.append(",");
			sb.append(e.getKey()).append(":").append(e.getValue());
		}
		return sb.toString();
	}

	public String getUid() {
		return prefs.get("uid");
	}

	public void setUid(String uid) {
		prefs.put("uid", uid);
	}

	public String getGroup() {
		return prefs.get("group");
	}

	public void setGroup(String pgid) {
		prefs.put("group", pgid);
	}

	public String getSupplementaryGroups() {
		return prefs.get("supgids");
	}

	public void setSupplementaryGroups(String... supgids) {
		prefs.put("supgids", String.join("+", supgids));
	}

	public String getRole() {
		return prefs.get("role");
	}

	public void setRole(String role) {
		prefs.put("role", role);
	}

	public void clear(){
		prefs.clear();
	}
	
	public void put(String name, String value) {
		prefs.put(name, value);
	}
	
	public String get(String name) {
		return prefs.get(name);
	}
}
