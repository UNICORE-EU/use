package eu.unicore.services.admin;

import java.util.HashMap;
import java.util.Map;

/**
 * The result of invoking an {@link AdminAction}
 *
 * @author schuller
 */
public class AdminActionResult {

	private final Map<String,String>results = new HashMap<>();

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
		results.put(name,value);
	}

}
