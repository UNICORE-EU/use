package eu.unicore.services.rest.admin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.impl.BaseRESTController;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.utils.MetricUtils;
import eu.unicore.util.Log;

/**
 * REST interface to the admin features of the container
 *
 * @author schuller
 */
@Path("/")
public class Admin extends BaseRESTController {
	
	private static final Logger logger = Log.getLogger(Log.SERVICES, Admin.class);
	
	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> status = new HashMap<>();
		status.put("upSince", String.valueOf(kernel.getUpSince().getTime()));
		status.put("containerVersion", String.valueOf(Kernel.getVersion()));
		status.put("connectionStatus", getConnectionStatus());
		status.put("metrics",MetricUtils.getValues(kernel.getMetrics()));
		return status;
	}
	
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	public String getHTMLRepresentation() throws Exception {
		return getHTML();
	}
	
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJSONRepresentation(@QueryParam("fields")String fields) throws Exception {
		try{
			parsePropertySpec(fields);
			JSONObject o = getJSON();
			ResponseBuilderImpl res = new ResponseBuilderImpl();
			res.status(Status.OK);
			res.type(MediaType.APPLICATION_JSON);
			res.entity(o.toString());
			return res.build();
		}
		catch(Exception ex){
			return handleError("Error getting properties", ex, logger);
		}
	}
	
	protected Map<String,String> getConnectionStatus(){
		Map<String,String> connectionStatus = new HashMap<>();
		for(ISubSystem sub: kernel.getSubSystems()){
			for(ExternalSystemConnector conn: sub.getExternalConnections()) {
				connectionStatus.put(conn.getExternalSystemName(), conn.getConnectionStatusMessage());
			}
		}
		return connectionStatus;
	}
	
	@Override
	protected void updateLinks(){
		super.updateLinks();
		Collection<AdminAction> actions = kernel.getAdminActions().values();
		for(AdminAction adm: actions){
			String name = adm.getName();
			links.add(new Link("action:"+name, getBaseURL()+"/actions/"+name, adm.getDescription()));
		}
	}
	
	/**
	 * handles actions, accepting an optional JSON document
	 */
	@POST
	@Path("/actions/{action}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response handleAction(@PathParam("action") String action, String json) throws Exception {
		try {
			JSONObject param = json!=null ? new JSONObject(json) : new JSONObject();
			JSONObject reply = doHandleAction(action, param);
			ResponseBuilder rb = Response.ok().entity(reply.toString()).type("application/json");
			rb.type("application/json");
			return rb.build();
		} catch (Exception ex) {
			return handleError("Error handling action: '" + action + "'", ex, logger);
		}
	}

	protected JSONObject doHandleAction(String action, JSONObject param) throws Exception {
		JSONObject reply = new JSONObject();
		AdminAction adm = kernel.getAdminActions().get(action);
		if(adm==null)throw new WebApplicationException(404);

		Map<String,String>params = RESTUtils.asMap(param);
		Client client = AuthZAttributeStore.getClient();
		
		logger.info("Invoking administrative action <"+action+"> :"
				+" client='"+client.getDistinguishedName()+"'"
				+" role="+client.getRole().getName() + " parameters="+params);
		
		AdminActionResult admResult = adm.invoke(params, kernel);
		
		logger.info("Administrative action <"+action+"> success="+admResult.successful()
				+" message='"+admResult.getMessage()+"' results="+admResult.getResults()
				+" resultReferences="+admResult.getResultReferences());
		
		reply.put("success", admResult.successful());
		reply.put("message", admResult.getMessage());
		JSONObject results = new JSONObject();
		reply.put("results", results);
		for(Map.Entry<String, String> res: admResult.getResults().entrySet()){
			results.put(res.getKey(), res.getValue());
		}
		return reply;
	}

}
