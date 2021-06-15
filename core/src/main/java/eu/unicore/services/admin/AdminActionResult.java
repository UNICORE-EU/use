package eu.unicore.services.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of invoking an {@link AdminAction}
 *
 * @author schuller
 */
public class AdminActionResult {

	private final Map<String,String>results=new HashMap<String,String>();
	
	private final List<Object>resultReferences=new ArrayList<Object>();
	
	private final boolean success;
	
	private final String message;
	
	public AdminActionResult(boolean success, String message){
		this.success=success;
		this.message=message;
	}
	
	public boolean successful(){
		return success;
	}
	
	public String getMessage(){
		return message;
	}
	
	public Map<String,String>getResults(){
		return results;
	}
	
	public void addResult(String name, String value){
		if(value==null)results.remove(name);
		results.put(name,value);
	}
	
	public List<Object>getResultReferences(){
		return resultReferences;
	}
	
	public void addResultReference(Object reference){
		resultReferences.add(reference);
	}
	
}
