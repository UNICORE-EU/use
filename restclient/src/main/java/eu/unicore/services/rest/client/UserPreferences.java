package eu.unicore.services.rest.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpMessage;

public class UserPreferences {

	protected final Map<String,String> prefs = new HashMap<>();
	
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

	public String getPgid() {
		return prefs.get("pgid");
	}

	public void setPgid(String pgid) {
		prefs.put("pgid", pgid);
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
}
