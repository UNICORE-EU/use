package eu.unicore.services.rest.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.persist.PersistenceException;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.RegistryClient;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
			RegistryImpl sg = (RegistryImpl)resource;
			Map<String,String>content = BaseClient.asMap(j);
			String endpoint = j.getString(RegistryClient.ENDPOINT);
			if(endpoint==null) {
				throw new WebApplicationException("Registry entry must specify an 'Endpoint'",
						HttpStatus.SC_BAD_REQUEST);
			}
			ContainerProperties cfg = kernel.getContainerProperties(); 
			long refreshIn = cfg.getLongValue(ContainerProperties.WSRF_SGENTRY_TERMINATION_TIME);
			String entryID = sg.addEntry(endpoint, content, null);
			String location = "/"+entryID;
			LocalRegistryClient lrc = new LocalRegistryClient(home.getServiceName(), resource.getUniqueID(), kernel);
			lrc.invalidateCache();
			return Response.created(new URI(location))
					.header("X-UNICORE-Lifetime", String.valueOf(refreshIn))
					.build();			
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
		if(wantProperty("entries")) {
			LocalRegistryClient lrc = new LocalRegistryClient(home.getServiceName(), resource.getUniqueID(), kernel);
			List<Object>entries = new ArrayList<>();
			for(Map<String,String> e: lrc.listEntries()){
				Map<String,Object> restEntry = renderEntry(e);
				if(restEntry!=null)entries.add(restEntry);
			}
			status.put("entries", entries);
		}
		return status;
	}

	protected Map<String,Object>renderEntry(final Map<String,String> value){
		Map<String,Object> map = new HashMap<>();
		String endpoint = value.get(RegistryClient.ENDPOINT);
		map.put("href",endpoint);
		map.putAll(value);
		String interfaceName = value.get("InterfaceName");
		if(interfaceName!=null) {
			map.put("type",interfaceName);
			map.remove(RegistryClient.INTERFACE_NAME);
		}
		map.remove(RegistryClient.ENDPOINT);
		return map;
	}

	@Override
	protected void customizeBaseProperties(JSONObject props) throws Exception {
		if(wantProperty("client"))props.put("client", ApplicationBaseResource.getBaseClientProperties());
		if(wantProperty("server"))props.put("server", ApplicationBaseResource.getBaseServerProperties(kernel));
	}

}
