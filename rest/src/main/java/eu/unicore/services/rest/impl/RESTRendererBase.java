package eu.unicore.services.rest.impl;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils.HtmlBuilder;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Helper class providing help for the basic rendering of REST resources in
 * JSON and HTML, including rendering of links
 *
 * @author schuller
 */
public abstract class RESTRendererBase implements KernelInjectable {

	/**
	 * the base URL of this resource (including the REST service name),
	 * it will be injected by the container 
	 */
	protected String baseURL;

	/**
	 *  injected by the container
	 */
	protected Kernel kernel;

	protected Collection<Link> links = new ArrayList<Link>();

	protected String resourceID;
	
	/**
	 * list of result fields requested in a GET - if empty, 
	 * the user wants all the fields
	 */
	protected final List<String> requestedProperties = new ArrayList<>();
	
	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	
	@Override
	public void setKernel(Kernel kernel){
		this.kernel = kernel;
	}

	public boolean usesKernelMessaging() {
		return false;
	}

	/**
	 * Update/create the links for the current state of the resource.
	 * (Invoked when the resource's representation is retrieved)
	 */
	protected void updateLinks(){

	}

	protected abstract Map<String,Object>getProperties() throws Exception;

	protected JSONObject getJSON() throws Exception{
		JSONObject o = new JSONObject();
		renderJSONProperties(o);
		renderJSONLinks(o);
		return o;
	}

	protected void parsePropertySpec(String fields) {
		if(fields!=null){
			for(String f: fields.split("[ +,]"))requestedProperties.add(f);
		}
	}
	protected boolean wantProperty(String property) {
		return requestedProperties.size()==0 || requestedProperties.contains(property);
	}
	
	protected void renderJSONProperties(JSONObject o) throws Exception {
		Map<String,Object> status = getProperties();
		for(Map.Entry<String,Object> e: status.entrySet()){
			String k = e.getKey();
			if(wantProperty(k)) {
				o.put(k, getJSONObject(e.getValue()));
			}
		}
	}

	protected void renderJSONLinks(JSONObject o) throws Exception {
		if(!wantProperty("_links")) {
			return;
		}
		updateLinks();
		JSONObject linkCollection = new JSONObject();
		for(Link link: links){
			linkCollection.put(link.getRelation(), renderJSONLink(link));
		}
		o.put("_links", linkCollection);
	}

	protected JSONObject renderJSONLink(Link link) throws Exception {
		JSONObject jsonLink = new JSONObject();
		jsonLink.put("href", link.getHref());
		if(link.getDescription()!=null){
			jsonLink.put("description", link.getDescription());
		}
		return jsonLink;
	}

	@SuppressWarnings("unchecked")
	protected Object getJSONObject(Object value) throws Exception{
		if(value instanceof Collection<?>){
			JSONArray o = new JSONArray();
			Collection<Object> collection = (Collection<Object>)value;
			for(Object e: collection){
				o.put(getJSONObject(e));
			}
			return o;
		}
		else if(value instanceof Map<?, ?>){
			JSONObject o = new JSONObject();
			Map<String,Object> map = (Map<String,Object>)value;
			for(Map.Entry<String,Object> e: map.entrySet()){
				o.put(e.getKey(), getJSONObject(e.getValue()));
			}
			return o;
		}
		else return value;
	}

	protected String getHTML() throws Exception{
		HtmlBuilder b = new HtmlBuilder();
		b.h(2, "Properties");
		renderHTMLProperties(b);
		b.h(2, "Links");
		renderHTMLLinks(b);
		return b.build();
	}

	protected void renderHTMLProperties(HtmlBuilder b) throws Exception {
		Map<String,Object> status = getProperties();
		b.table();
		for(Map.Entry<String,Object> e: status.entrySet()){
			b.tr();
			b.td().bftext(e.getKey()).end();
			b.td().text(getHTMLValue(e.getValue(), 0)).end();
			b.end();
		}
		b.end();
	}

	protected void renderHTMLLinks(HtmlBuilder b) throws Exception {
		updateLinks();
		b.table();
		if(links.size()>0){
			b.tr();
			b.th().text("Relation").end();
			b.th().text("Link").end();
			b.end();
		}
		for(Link link: links){
			b.tr();
			String rel = link.getRelation();
			String href = link.getHref();
			b.td().text(rel).end();
			if(rel.startsWith("action")){
				b.td();
				b.text("<form action='").text(href).text("' method='post'>");
				b.text("<input type='submit'");
				b.text(" value='").text(link.getDescription()).text("'>");
				b.text("</form>");
				b.end();
			}
			else{
				String text = link.getDescription();
				if(text==null)text = href;
				b.td().href(href, text).end();
			}
			b.end();
		}
		b.end();
	}

	@SuppressWarnings("unchecked")
	protected String getHTMLValue(Object value, int nestingLevel) throws Exception{
		HtmlBuilder b = null;
		if(value instanceof Collection<?>){
			b = new HtmlBuilder(true);
			Collection<Object> collection = (Collection<Object>)value;
			b.ul();
			for(Object e: collection){
				b.li();
				b.text(getHTMLValue(e, nestingLevel+1));
				b.end();
			}
			b.end();
		}
		else if(value instanceof Map<?, ?>){
			b = new HtmlBuilder(true);
			Map<Object,Object> map = (Map<Object,Object>)value;
			b.ul();
			for(Map.Entry<Object,Object> e: map.entrySet()){
				b.li();
			    if(nestingLevel<1) {
			    	b.bftext(String.valueOf(e.getKey()));
			    }else {
			    	b.text(String.valueOf(e.getKey())+":");
			    }
				b.text("&nbsp;");
				b.text(getHTMLValue(e.getValue(), nestingLevel+1));
				b.end();
			}
			b.end();
		}

		else{
			String text = String.valueOf(value);
			if(text.toLowerCase().startsWith("http")){
				text = "<a href='"+text+"'>"+text+"</a>";
			}
			return text;
		}

		return b.build();
	}
	
	private DateFormat iso8601 = null;

	protected synchronized DateFormat getISODateFormatter(){
		if(iso8601 == null){
			iso8601 = Utilities.getISO8601();
		}
		return iso8601;
	}
	
	/**
	 * Create an error (500) Response. 
	 * The error info will be placed in the response body as JSON.
	 */
	public static Response handleError(String message, Throwable cause, Logger logger) 
			throws WebApplicationException{
		return handleError(500, message, cause, logger);
	}	

	/**
	 * Create an error Response with the given HTTP status. 
	 * The error info will be placed in the response body as JSON.
	 */
	public static Response handleError(int status, String message, Throwable cause, Logger logger) 
			throws WebApplicationException{
		if(cause!=null && cause instanceof WebApplicationException){
			// special case: no need to wrap these
			throw (WebApplicationException)cause;
		}
		String msg = null;
		if(cause!=null) {
			msg  = Log.createFaultMessage(message, cause);
			if(status>499 && logger!=null)Log.logException(message, cause, logger);
		}
		else msg = message;
		
		return createErrorResponse(status, msg);
	}

	/**
	 * Create a Response with the given HTTP status. 
	 * The error info will be placed in the response body as JSON.
	 */
	public static Response createErrorResponse(int status, String message) {
		ResponseBuilderImpl res = new ResponseBuilderImpl();
		res.status(status);
		res.type(MediaType.APPLICATION_JSON);
		JSONObject json = new JSONObject();
		try{
			json.put("errorMessage", message);
			json.put("status", status);
		}catch(Exception ex){}
		res.entity(json.toString());
		return res.build();
	}	
}

