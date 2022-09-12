package eu.unicore.services.rest.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.security.Client;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExtendedResourceStatus;
import eu.unicore.services.Home;
import eu.unicore.services.Model;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.BaseModel;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.impl.SecuredResourceImpl;
import eu.unicore.services.impl.SecuredResourceModel;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.utils.UnitParser;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * Base class for REST resources. 
 * 
 * Allows to inject service home, the resource instance, the 
 * resource model and the kernel.
 * 
 * Provides base implementations for GET (JSON and HTML) 
 * and DELETE.
 *
 * @author schuller
 */
public abstract class BaseRESTController extends RESTRendererBase {

	private static final Logger logger = Log.getLogger(Log.SERVICES, BaseRESTController.class);
	
	/**
	 *  injected by the container
	 */
	protected Model model;

	/**
	 *  injected by the container
	 */
	protected Resource resource;

	/**
	 *  injected by the container
	 */
	protected Home home;

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setModel(Model model){
		this.model = model;
		this.resourceID = model.getUniqueID();
	}

	public Model getModel(){
		return model;
	}

	public void setHome(Home home){
		this.home = home;
	}

	public Home getHome(){
		return home;
	}

	/**
	 * retrieve the resource's representation in JSON format
	 */
	@GET
	@Path("/{uniqueID}")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response getJSONRepresentation(@PathParam("uniqueID") String id, @QueryParam("fields")String fieldSpec) throws Exception {
		try{
			parsePropertySpec(fieldSpec);
			ResponseBuilderImpl res = new ResponseBuilderImpl();
			res.status(Status.OK);
			res.type(MediaType.APPLICATION_JSON);
			res.entity(getJSON().toString());
			return res.build();
		}
		catch(Exception ex){
			return handleError("Error getting resource state", ex, logger);
		}
	}

	/**
	 * retrieve the resource's representation in HTML format
	 */
	@GET
	@Path("/{uniqueID}")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public Response getHTMLRepresentation(@PathParam("uniqueID") String id) throws Exception {
		try{
			ResponseBuilderImpl res = new ResponseBuilderImpl();
			res.status(Status.OK);
			res.type(MediaType.TEXT_HTML);
			res.entity(getHTML());
			return res.build();
		}
		catch(Exception ex){
			return handleError("Error getting resource state", ex, logger);
		}
	}

	@DELETE
	@Path("/{uniqueID}")
	public void destroy(@PathParam("uniqueID") String id) throws Exception {
		assertOwnerLevelAccess();
		resource.destroy();
		home.destroyResource(id);
		try{
			String owner = ((SecuredResourceModel)model).getOwnerDN();
			((DefaultHome)home).instanceDestroyed(owner);
		}catch(Exception ex){}
	}
	
	/**
	 * update resource properties
	 * 
	 * @param json - JSON representation of resource properties
	 * @return JSON showing what was set and what not
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@PUT
	@Path("/{uniqueID}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String setProperties(@PathParam("uniqueID") String id, String json) throws Exception {
		JSONObject o = new JSONObject(json);
		Iterator<?> i = o.keys();
		JSONObject reply = new JSONObject();
		while(i.hasNext()){
			boolean found = false;
			String propertyName = String.valueOf(i.next());
			// value can be a JSONList,  JSONObject or just a String
			try{
				Object val = o.get(propertyName);
				if(val instanceof String){
					found = doSetProperty(propertyName,(String)val);
				}
				else if(val instanceof JSONArray){
					found = doSetProperty(propertyName,(JSONArray)val);
				}
				else if(val instanceof JSONObject){
					found = doSetProperty(propertyName,(JSONObject)val);
				}
				if(!found){
					reply.put(propertyName, "Property not found or cannot be modified!");
				}
				else{
					reply.put(propertyName, "OK");
				}
			}catch(Exception ex){
				reply.put(propertyName, Log.createFaultMessage("Error setting property", ex));
			}
		}
		return reply.toString();
	}

	/**
	 * set a property
	 * @param name - property name
	 * @param value - property value
	 * @return <code>true</code> if property is known and was set correctly,
	 *         <code>false</code> if property name is not known
	 * @throws Exception in case of errors setting the property
	 */
	protected boolean doSetProperty(String name, String value) throws Exception {
		if("acl".equals(name)){
			List<String> update = new ArrayList<>();
			update.add(value);
			setACL(update);
			return true;
		}
		if("terminationTime".equals(name)){
			Calendar newTT = Calendar.getInstance();
			newTT.setTime(UnitParser.extractDateTime(value));
			home.setTerminationTime(resource.getUniqueID(), newTT);
			return true;
		}
		if("tags".equals(name)){
			String[]tags = value.split("[ +,]");
			model.getTags().clear();
			model.getTags().addAll(Arrays.asList(tags));
			return true;
		}
		return false;
	}

	/**
	 * set a property to the given values
	 * @param name - property name
	 * @param value - array of property values
	 * @return <code>true</code> if property is known and was set correctly,
	 *         <code>false</code> if property name is not known
	 * @throws Exception in case of errors setting the property
	 */
	protected boolean doSetProperty(String name, JSONArray value) throws Exception {
		if("acl".equals(name)){
			List<String> update = new ArrayList<>();
			for(int i=0;i<value.length();i++){
				update.add(String.valueOf(value.get(i)));
			}
			setACL(update);
			return true;
		}
		if("tags".equals(name)){
			List<String> update = new ArrayList<>();
			for(int i=0;i<value.length();i++){
				update.add(String.valueOf(value.get(i)));
			}
			model.getTags().clear();
			model.getTags().addAll(update);
			return true;
		}
		
		return false;
	}

	/**
	 * set a property to the given (complex) value
	 * @param name - property name
	 * @param value - property value
	 * @return <code>true</code> if property is known and was set correctly,
	 *         <code>false</code> if property name is not known
	 * @throws Exception in case of errors setting the property
	 */
	protected boolean doSetProperty(String name, JSONObject value) throws Exception {
		return false;
	}

	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = new HashMap<>();
		Model model = getModel();
		String resourceID = model.getUniqueID();
		if(model instanceof SecuredResourceModel){
			SecuredResourceModel srm = (SecuredResourceModel)model;
			String owner = srm.getOwnerDN();
			if(owner == null){
				try{
					owner = kernel.getContainerSecurityConfiguration().getCredential().getSubjectName();
				}catch(Exception ex){}
			}
			props.put("owner", owner);
			props.put("acl", renderACL(srm.getAcl()));
			Calendar tt = home.getTerminationTime(resourceID);
			if(tt!=null){
				props.put("terminationTime", getISODateFormatter().format(tt.getTime()));
			}
			props.put("currentTime", getISODateFormatter().format(new Date()));
		}
		if(model instanceof BaseModel){
			BaseModel bm = (BaseModel)model;
			props.put("resourceStatus", String.valueOf(bm.getResourceStatus()));
			props.put("resourceStatusMessage", String.valueOf(bm.getResourceStatusDetails()));
			
		}
		if(resource!=null && resource instanceof ExtendedResourceStatus){
			ExtendedResourceStatus esr = (ExtendedResourceStatus)resource;
			props.put("resourceStatus", String.valueOf(esr.getResourceStatus()));
			props.put("resourceStatusMessage", esr.getStatusMessage());
		}
		props.put("tags", model.getTags());
		try {
			String name = kernel.getContainerProperties().getValue(ContainerProperties.VSITE_NAME_PROPERTY);
			if(name!=null)props.put("siteName", name);	
		}catch(Exception ex) {}
		return props;
	}

	protected List<String>renderACL(List<ACLEntry>acl){
		List<String> res = new ArrayList<>();
		for(ACLEntry e: acl){
			res.add(e.getAccessType()+":"+e.getMatchType()+":"+e.getRequiredValue());
		}
		return res;
	}
	
	protected boolean setACL(List<String>acl){
		if(model instanceof SecuredResourceModel){
			assertOwnerLevelAccess();
			List<ACLEntry> update = new ArrayList<>();
			for(String e: acl){
				ACLEntry x = ACLEntry.parse(e);
				update.add(x);
			}
			List<ACLEntry>current = ((SecuredResourceModel) model).getAcl();
			current.clear();
			current.addAll(update);
			return true;
		}
		else return false;
	}
	
	/**
	 * get the IDs of the subset of Resources (managed by Home) that
	 * is accessible to the current client
	 * 
	 * @param tagSpec optional tags (space or comma separated)
	 * @throws PersistenceException
	 */
	protected List<String>getAccessibleResources(String tagSpec) throws Exception { 
		Client c=AuthZAttributeStore.getClient(); 
		
		if(tagSpec!=null){
			String[] tags = tagSpec.split("[ +,]");
			List<String> tagged = home.getStore().getTaggedResources(tags);
			return home.getAccessibleResources(tagged, c);
		}
		else{
			return home.getAccessibleResources(c);
		}
	}

	protected void assertOwnerLevelAccess() {
		if(resource instanceof SecuredResourceImpl){
			if(!((SecuredResourceImpl)resource).isOwnerLevelAccess()){
				throw new WebApplicationException(403);
			}
		}
	}

}

