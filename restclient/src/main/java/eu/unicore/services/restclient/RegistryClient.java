package eu.unicore.services.restclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * access to the Registry
 * 
 * @author schuller
 */
public class RegistryClient extends BaseClient {

	public RegistryClient(String url, IClientConfiguration security) {
		this(url, security, null);
	}

	public RegistryClient(String url, IClientConfiguration security, IAuthCallback authCallback) {
		super(url, security, authCallback);
	}

	/**
	 * Add an entry. Reply is the "lifetime" of the entry in seconds, 
	 * ie. the time the entry will be retained.
	 * 
	 * @param content
	 * @return lifetime in seconds
	 * @throws Exception
	 */
	public long addEntry(Map<String,String>content) throws Exception {
		try(ClassicHttpResponse response = post(asJSON(content))){
			return Long.parseLong(response.getHeaders("X-UNICORE-Lifetime")[0].getValue());
		}catch(Exception ex) {
			return -1;
		}
	}

	public List<JSONObject> listEntries() throws Exception {
		return listEntries(null);
	}

	public List<JSONObject> listEntries(String serviceType) throws Exception {
		JSONObject props = getJSON();
		List<JSONObject>result = new ArrayList<>();
		JSONArray entries = props.getJSONArray("entries");
		for(int i=0; i<entries.length(); i++){
			try{
				JSONObject o = entries.getJSONObject(i);
				if(serviceType==null) {
					result.add(o);
				}
				else {
					String type = o.optString("type", o.optString(INTERFACE_NAME, null));
					if(type!=null && serviceType.equalsIgnoreCase(type)){
						result.add(o);
					}
				}
			}catch(Exception ex){}
		}
		return result;
	}

	// constants for storing standard info in the content map
	public static final String ENDPOINT = "Endpoint";
	public static final String INTERFACE_NAME = "InterfaceName";
	public static final String INTERFACE_NAMESPACE = "InterfaceNamespace";
	public static final String SERVER_IDENTITY = "ServerIdentity";
	public static final String SERVER_PUBKEY = "ServerPublicKey";
	public static final String ENTRY_ID = "EntryID";

}
