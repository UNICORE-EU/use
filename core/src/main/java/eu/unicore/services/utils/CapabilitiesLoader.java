package eu.unicore.services.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.util.Log;

/**
 * helper to load the available capabilities from the class path
 * 
 * @author schuller
 */
public class CapabilitiesLoader {

	private static final Logger logger = Log.getLogger(Log.UNICORE, CapabilitiesLoader.class);

	private CapabilitiesLoader(){}
	
	/**
	 * @return a unmodifiable map of Capability instances keyed with the name
	 */
	public static Map<String, Capability>load(Kernel kernel){
		Map<String,Capability>result=new HashMap<String,Capability>();
		ServiceLoader<Capabilities> sl=ServiceLoader.load(Capabilities.class);
		Iterator<Capabilities>iter=sl.iterator();
		while(iter.hasNext()){
			Capabilities cc=iter.next();
			for(Capability c : cc.getCapabilities()){
				if(c instanceof KernelInjectable){
					((KernelInjectable)c).setKernel(kernel);
				}
				result.put(c.getName(), c);
				logger.debug("Loaded capability {}", c.getName());
			}
		}
		return Collections.unmodifiableMap(result);
	}
}
