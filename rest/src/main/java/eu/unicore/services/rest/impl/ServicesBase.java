package eu.unicore.services.rest.impl;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.persist.PersistenceException;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * Provides support for typical (WSRF-like) services, where the base resource
 * can list its children. Additionally, provides support for actions.
 *  
 * @author schuller
 */
public abstract class ServicesBase extends BaseRESTController {

	protected static final Logger logger = Log.getLogger("unicore.rest", ServicesBase.class);

	/**
	 * returns the name for the child resources: by default this will be the
	 * value of the @Path annotation of this class without any leading "/", 
	 * or <code>null</code> if not set
	 */
	protected String getResourcesName(){
		Path p = getClass().getAnnotation(Path.class);
		if(p==null)return null;
		
		if(p.value().startsWith("/")){
			return p.value().substring(1);
		}
		else{
			return p.value();
		}
	}

	/**
	 * returns the path component used in hrefs
	 */
	protected String getPathComponent(){
		String rn = getResourcesName();
		return rn!=null ? rn +"/" : "";
	}

	/**
	 * list children (JSON)
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response listAll(
			@QueryParam("offset") @DefaultValue(value="0") int offset, 
			@QueryParam("num") @DefaultValue(value="200") int num,
			@QueryParam("tags") String tags,
			@QueryParam("filter") String filter
			) throws Exception {
		try{
			List<String>uids = getAccessibleResources(tags);
			Predicate<String> f = createFilter(filter);
			if(f!=null)uids.removeIf(f);
			String base = getBaseURL()+"/"+getPathComponent();
			PagingHelper ph = new PagingHelper(base, base, getResourcesName());
			JSONObject o = ph.renderJson(offset, num, uids);
			customizeBaseProperties(o);

			return Response.ok(o.toString(), MediaType.APPLICATION_JSON).build();

		}catch(Exception ex){
			return handleError("Error", ex, logger);
		}
	}

	/**
	 * allows subclasses to create a filter to remove certain
	 * elements from the list of all accessible resources returned
	 * by the listAll() function
	 *
	 * @param filterSpec (can be null)
	 */
	protected Predicate<String> createFilter(String filterSpec){
		return null;
	}

	/**
	 * allows subclasses to customize the JSON returned by the 
	 * generic listAll() method
	 * 
	 * @param baseProperties
	 */
	protected void customizeBaseProperties(JSONObject baseProperties) throws Exception {}
	
	/**
	 * list all children (HTML)
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public String listAllHtml() throws Exception {
		RESTUtils.HtmlBuilder b = new RESTUtils.HtmlBuilder();
		List<String> uids = getAccessibleResources(null);
		Predicate<String> f = createFilter(null);
		if(f!=null)uids.removeIf(f);
		for (String id : getAccessibleResources(null)) {
			String url = getBaseURL() + "/" + getPathComponent() + id;
			b.href(url, id);
			b.br();
		}
		return b.build();
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		links.add(new Link("self", getBaseURL() + "/" + getPathComponent() + resource.getUniqueID()));
	}

	/**
	 * handles actions (when triggered via html form)
	 */
	@POST
	@Path("/{uniqueID}/actions/{action}")
	@Consumes("application/x-www-form-urlencoded")
	public Response handleActionByBrowser(@PathParam("action") String action) throws Exception {
		try {
			doHandleAction(action, new JSONObject());
			ResponseBuilder rb = Response
					.seeOther(new URI(getBaseURL() + "/" + getPathComponent() + resource.getUniqueID()));
			rb.type("text/html");
			return rb.build();
		} catch (Exception ex) {
			return handleError("Error handling action: '" + action + "'", ex, logger);
		}
	}

	/**
	 * handles actions, accepting an optional JSON document
	 */
	@POST
	@Path("/{uniqueID}/actions/{action}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleAction(@PathParam("action") String action, String json) throws Exception {
		try {
			JSONObject param = (json!=null && json.length()>0) ? new JSONObject(json) : new JSONObject();
			JSONObject reply = doHandleAction(action, param);
			ResponseBuilder rb = reply==null ? Response.noContent() : Response.ok(reply.toString());
			rb.type("application/json");
			return rb.build();
		} catch (Exception ex) {
			return handleError("Error handling action: '" + action + "'", ex, logger);
		}
	}

	/**
	 * handle the named action
	 * @return a reply (can be null) to return to the client
	 */
	protected JSONObject doHandleAction(String name, JSONObject o) throws Exception {
		throw new IllegalArgumentException("Undefined!");
	}
}
