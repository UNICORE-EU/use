package eu.unicore.services.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.wsrflite.registry.ServiceRegistryImpl;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.impl.ServicesBase;

/**
 * @author schuller
 */
@Path("/")
@USEResource(home="Registry")
public class Registries extends ServicesBase {

	@Override
	protected String getResourcesName() {
		return "registries";
	}

	@Override
	protected String getPathComponent() {
		return "";
	}
	
	/**
	 * post a new entry to the registry (or update an existing one)
	 *
	 * @param json - JSON registry entry data
	 * @return address of new resource in the Location header
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@POST
	@Path("/{uniqueID}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addEntry(String json) throws Exception {
		try{
			JSONObject j = new JSONObject(json);
			ServiceRegistryImpl sg = (ServiceRegistryImpl)resource;
			Map<String,String>content = BaseClient.asMap(j);
			String endpoint = j.getString(RegistryClient.ENDPOINT);
			String entryID = sg.addEntry(endpoint, content, null);
			String location = "/"+entryID;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create/update registry entry", ex, logger);
		}
	}
	
	/**
	 * render properties of a given Registry instance
	 */
	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> status = super.getProperties();
		ServiceRegistryImpl sg = (ServiceRegistryImpl)resource;
		List<Object>entries = new ArrayList<>();
		for(Map.Entry<String,Map<String,String>> e: sg.getModel().getContents().entrySet()){
			Map<String,Object> restEntry = renderEntry(e.getKey(),e.getValue());
			if(restEntry!=null)entries.add(restEntry);
		}
		status.put("entries", entries);
		return status;
	}

	protected Map<String,Object>renderEntry(String endpoint, Map<String,String> value){
		Map<String,Object> map = new HashMap<String,Object>();
		String href = convertToREST(endpoint);
		if(href!=null){
			map.put("wsrf",endpoint);
			map.put("href",href);
		}
		else{
			// non wsrf link
			map.put("href",endpoint);	
		}
		String interfaceName = value.get(RegistryClient.INTERFACE_NAME);
		if(interfaceName!=null)map.put("type",interfaceName);
		String dn = value.get(RegistryClient.SERVER_IDENTITY);
		if(dn!=null)map.put(RegistryClient.SERVER_IDENTITY,dn);
		String pubkey = value.get(RegistryClient.SERVER_PUBKEY);
		if(pubkey!=null)map.put(RegistryClient.SERVER_PUBKEY,pubkey);
		
		return map;
	}
	
	private static final Pattern wsrfURLPattern = Pattern.compile("(https||http)://(.*)/services/([^?]*)\\?res=(.*)");
	
	private static final Map<String,String>conv = new HashMap<String,String>();
	
	static{
		conv.put("StorageManagement", "core/storages");
		conv.put("TargetSystemFactoryService", "core/factories");
		conv.put("StorageFactory", "core/storagefactories");
		conv.put("WorkflowFactory", "workflows");
	}
	
	/**
	 * Converts a UNICORE WSRF URL to a UNICORE REST URL
	 * (heuristically, i.e. using pattern matching)
	 */
	public static String convertToREST(String wsrfURL){
		Matcher m = wsrfURLPattern.matcher(wsrfURL);
		if(!m.matches())return null;
		String scheme=m.group(1);
		String base=m.group(2);
		String restPathElement=conv.get(m.group(3));
		if(restPathElement==null)return null;
		String resID=m.group(4);
		String restURL = scheme+"://"+base+"/rest/"+restPathElement+"/"+resID;
		return restURL;
	}
	
	public static boolean isWSRFEndpoint(String endpoint){
		Matcher m = wsrfURLPattern.matcher(endpoint);
		return m.matches();
	}
}