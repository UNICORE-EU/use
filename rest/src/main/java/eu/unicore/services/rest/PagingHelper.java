package eu.unicore.services.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.services.rest.RESTUtils.HtmlBuilder;

/**
 * helper for returning pages of results including next/previous links
 *
 * @author schuller
 */
public class PagingHelper {

	String linkBase, childURLBase, childResourceName;
	
	/**
	 * @param linkBase - base URL used for the next, prev and self links
	 * @param childURLBase - base URL for the links to child resources. These will get the child ID appended. Can be empty ("")
	 * @param childResourceName - the JSON tag to use in the JSON properties for the list of children
	 */
	public PagingHelper(String linkBase, String childURLBase, String childResourceName){
		this.linkBase = linkBase;
		if(childURLBase.length()>0) {
			this.childURLBase = childURLBase.endsWith("/") ? childURLBase : childURLBase+"/";
		}else {
			this.childURLBase = childURLBase;
		}
		this.childResourceName = childResourceName;
	}
	
	/**
	 * Render a subset of the given list of UIDs. The resulting URLs will
	 * be composed from the childURLBase
	 * Also renders "_links".
	 * 
	 * @param offset - where to start
	 * @param num - how many to render
	 * @param objs - list of UIDs
	 * @return JSONObject results contains "_links" and the list of children
	 * @throws JSONException
	 */
	public JSONObject renderJson(int offset, int num, List<String>objs) throws JSONException {
		if(offset<0 || num < 0)throw new IllegalArgumentException("offset and num must be positive.");

		JSONObject result = new JSONObject();
		JSONArray children = new JSONArray();
		result.put(childResourceName!=null ? childResourceName : "children", children);
		int upper = Math.min(offset+num, objs.size());
		for(int i=offset; i<upper;i++){
			children.put(childURLBase + objs.get(i));
		}

		JSONObject linkCollection = new JSONObject();
		for(Link link: getLinks(offset,num, objs.size())){
			linkCollection.put(link.getRelation(), renderJSONLink(link));
		}
		result.put("_links", linkCollection);

		return result;
	}

	/**
	 * get "next", "previous", "self" links, as appropriate. 
	 * The base URL for these is the linkBase
	 *  
	 * @param offset
	 * @param num
	 * @param max
	 */
	public Collection<Link> getLinks(int offset, int num, int max){
		Collection<Link> links = new ArrayList<Link>();
		
		if(offset+num<max){
			int batch = Math.min(num, max-num);
			Link next = new Link("next",linkBase+"?offset="+(offset+num)+"&num="+batch);
			links.add(next);
		}
		if(offset>0){
			int prev = Math.max(offset-num, 0);
			Link previous= new Link("previous",linkBase+"?offset="+(prev)+"&num="+num);
			links.add(previous);
		}
		Link self= new Link("self",linkBase+"?offset="+(offset)+"&num="+num);
		links.add(self);

		return links;
	}

	protected JSONObject renderJSONLink(Link link) throws JSONException {
		JSONObject jsonLink = new JSONObject();
		jsonLink.put("href", link.getHref());
		if(link.getDescription()!=null){
			jsonLink.put("description", link.getDescription());
		}
		return jsonLink;
	}

	public String renderHTML(int offset, int num, List<String>objs) {
		HtmlBuilder b = new HtmlBuilder();
		
		b.h(2, childResourceName);
		int upper = Math.min(offset+num, objs.size());
		for(int i=offset; i<upper;i++){
			String id = objs.get(i);
			b.href(childURLBase + id, id);
			b.br();
		}
		
		b.h(2, "Links");
		renderHTMLLinks(b, offset, num, objs);
		return b.build();
	}

	protected void renderHTMLLinks(HtmlBuilder b, int offset, int num, List<String>objs) {
		Collection<Link> links =  getLinks(offset,num, objs.size());
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
			String text = link.getDescription();
			if(text==null)text = href;
			b.td().href(href, text).end();
			b.end();
		}
		b.end();
	}

}
