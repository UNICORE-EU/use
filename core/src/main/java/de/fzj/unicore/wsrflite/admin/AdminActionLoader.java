package de.fzj.unicore.wsrflite.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import eu.unicore.util.Log;

/**
 * helper to load admin actions from the class path
 * 
 * @author schuller
 */
public class AdminActionLoader {

	private static final Logger logger = Log.getLogger(Log.WSRFLITE, AdminActionLoader.class);

	private AdminActionLoader(){}
	
	/**
	 * returns an immutable map of {@link AdminAction} instances found
	 * on the classpath via the service loader mechanism
	 * 
	 * @return a unmodifiable map of AdminActions keyed with the name
	 */
	public static Map<String, AdminAction>load(){
		Map<String,AdminAction>result=new HashMap<String,AdminAction>();
		ServiceLoader<AdminAction> sl=ServiceLoader.load(AdminAction.class);
		Iterator<AdminAction>iter=sl.iterator();
		while(iter.hasNext()){
			AdminAction a=iter.next();
			result.put(a.getName(),a);
			logger.debug("Loaded admin action "+a.getName()+" "+a.getClass().getName());
		}
		return Collections.unmodifiableMap(result);
	}
}
