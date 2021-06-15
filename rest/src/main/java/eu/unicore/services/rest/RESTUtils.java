package eu.unicore.services.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.json.JSONObject;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.BaseClient;

public class RESTUtils {

	/**
	 * create a full URL to a REST resource
	 * @param kernel
	 * @param resourceBase
	 * @param id
	 */
	public static String makeHref(Kernel kernel, String resourceBase, String id){
		return kernel.getContainerProperties().getContainerURL()+"/rest/"+resourceBase+"/"+id;
	}
	

	/**
	 * create full URLs to REST resources
	 * @param kernel
	 * @param resourceBase
	 * @param ids
	 */
	public static Collection<String> makeHrefs(Kernel kernel, String resourceBase, Collection<String> ids){
		Collection<String>res = null;
		if(ids instanceof Set<?>){
			res = new HashSet<String>();
		}
		else{
			res = new ArrayList<String>();
		}
		for(String id: ids){
			res.add(kernel.getContainerProperties().getContainerURL()+"/rest/"+resourceBase+"/"+id);
		}
		return res;
	}
	
	
	public static Map<String,String>asMap(JSONObject o){
		return BaseClient.asMap(o);
	}


	public static class HtmlBuilder{
		
		private final StringBuilder sb = new StringBuilder();
		
		private final Stack<String> stack = new Stack<String>();
				
		private final boolean fragment;
		
		/**
		 * create a new HTMLBuilder
		 * @param fragment - if <code>true</code>, no enclosing html and body tags are used
		 */
		public HtmlBuilder(boolean fragment){
			this.fragment = fragment;
			if(!fragment){
				sb.append("<html>\n");
				sb.append("<head>");
				sb.append(getHeader());
				sb.append("</head>");
				sb.append("<body>\n");
			}
		}
		
		public HtmlBuilder(){
			this(false);
		}
		
		public HtmlBuilder h(int level, String heading){
			sb.append("<h").append(level).append(">");
			sb.append(heading);
			sb.append("</h").append(level).append(">");
			sb.append("\n");
			return this;
		}
		
		public HtmlBuilder href(String url, String text){
			sb.append("<a href='");
			sb.append(url);
			sb.append("'>").append(text).append("</a>");
			return this;
		}
		
		public HtmlBuilder br(){
			sb.append("<br/>\n");
			return this;
		}
		
		/**
		 * closes the innermost element
		 */
		public HtmlBuilder end(){
			String op = stack.pop();
			sb.append("</").append(op).append(">\n");
			return this;
		}
		
		public HtmlBuilder li(){
			sb.append("<li>");
			stack.push("li");
			return this;
		}
		
		public HtmlBuilder cr(){
			sb.append("\n");
			return this;
		}

		public HtmlBuilder ul(){
			sb.append("<ul>\n");
			stack.push("ul");
			return this;
		}
		
		public HtmlBuilder table(){
			sb.append("<table border=1 width='100%'>");
			stack.push("table");
			return this;
		}
		
		public HtmlBuilder tr(){
			sb.append("<tr>");
			stack.push("tr");
			return this;
		}
		
		public HtmlBuilder td(){
			sb.append("<td>");
			stack.push("td");
			return this;
		}
		
		public HtmlBuilder th(){
			sb.append("<th>");
			stack.push("th");
			return this;
		}

		/**
		 * add text
		 * @param item
		 */
		public HtmlBuilder text(String item){
			sb.append(item);
			return this;
		}
		
		/**
		 * add text in boldface
		 * @param item
		 */
		public HtmlBuilder bftext(String item){
			sb.append("<b>").append(item).append("</b>");
			return this;
		}
		
		/**
		 * render the final HTML
		 */
		public String build(){
			if(!fragment)sb.append("</body></html>");
			return sb.toString();
		}
		
		public String getHeader(){
			return "<style type=\"text/css\">" +
					"table, th, td { border: 1px solid black;  border-collapse: collapse;}" +
					"</style>";
		}

	}
}
