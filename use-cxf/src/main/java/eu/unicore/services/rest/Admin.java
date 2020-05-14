package eu.unicore.services.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.wsrflite.ExternalSystemConnector;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.utils.MetricUtils;
import eu.unicore.security.Client;
import eu.unicore.services.rest.impl.BaseRESTController;
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
		Map<String,Object> status = new HashMap<String, Object>();
		status.put("upSince", String.valueOf(kernel.getUpSince().getTime()));
		status.put("containerVersion", String.valueOf(Kernel.getVersion()));
		status.put("connectionStatus", getConnectionStatus());
		status.put("metrics",MetricUtils.getValues(kernel.getMetricRegistry()));
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
		for(ExternalSystemConnector conn: kernel.getExternalSystemConnectors()){
			connectionStatus.put(conn.getExternalSystemName(), conn.getConnectionStatusMessage());
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
