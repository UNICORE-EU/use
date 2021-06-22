package eu.unicore.services.rest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
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

	public void addEntry(Map<String,String>content) throws Exception {
		JSONObject obj = asJSON(content);
		HttpResponse response = post(obj);
		checkError(response);
		close(response);
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
				String type = o.optString("type", o.optString(INTERFACE_NAME, null));
				if(serviceType!=null && serviceType.equalsIgnoreCase(type)){
					result.add(o);
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

}
