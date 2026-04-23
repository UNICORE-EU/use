package eu.unicore.services.rest.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.services.registry.IRegistry;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.util.Log;

/**
 * A client for querying and adding to a set of external registries.
 * 
 * @author schuller
 */
public class ExternalRegistryClient implements IRegistry {

	private static final Logger logger = Log.getLogger(Log.CLIENT, ExternalRegistryClient.class);

	static final long readd_offset = 60;

	private final List<RegistryClient> clients = new ArrayList<>();

	public ExternalRegistryClient(){
		super();
	}

	public void addClient(RegistryClient client) {
		clients.add(client);
	}

	/**
	 * Adds the entry to all available external registries.
	 * This method will return the requested refresh instant,
	 * returned by any of the external registries, or a default
	 * of 5 minutes from now in case no external registry can be contacted.  
	 * 
	 * @return refresh instant
	 */
	public Calendar addRegistryEntry(Map<String,String> content){
		long refreshIn = doAddRegistryEntry(content);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis() + 1000*refreshIn);
		return c;
	}

	/*
	 * This method will return the requested time until refresh (in seconds)
	 * returned by any of the external registries, or a default of 5 minutes in case 
	 * no external registry can be contacted.  
	 */
	private long doAddRegistryEntry(Map<String,String> content){
		long refreshIn = 300;
		boolean haveExternalTT = false;
		for(RegistryClient reg: clients){
			String address = reg.getURL();
			try{
				logger.debug("Calling external registry: {}", address);
				reg.addEntry(content);
				long extRefresh = reg.addEntry(content);
				logger.debug("External registry {} requests lifetime: {} sec", address, extRefresh);
				if(extRefresh<0)extRefresh = 300;
				//select the "earliest" termination time reported back from the external registries
				//and subtract 60 "grace seconds"
				refreshIn = haveExternalTT ? 
						Math.min(refreshIn, extRefresh - readd_offset) : (extRefresh-readd_offset);
				if(!haveExternalTT){
					haveExternalTT = true;
				}
			}catch(Exception e){
				logger.debug("Error adding registry entry at <{}>: {}", address, e.getMessage());
			}
		}
		return refreshIn;
	}

	/**
	 * List all the entries in all the registries. No duplicate filtering
	 * is applied
	 */
	public List<Map<String,String>> listEntries() throws IOException {
		if(clients.size()==0) {
			throw new IOException("None of the configured registries is available.");
		}
		List<Map<String,String>> result = new ArrayList<>();
		StringBuilder errors = new StringBuilder();
		for(RegistryClient c: clients){
			try{
				for(JSONObject o: c.listEntries()) {
					result.add(RegistryClient.asMap(o));
				}
			}catch(Exception ex){
				String msg = String.format("Registry <%s> is not available: %s", 
						c.getURL(),
						Log.createFaultMessage("", ex));
				if(errors.length()>0)errors.append(". ");
				errors.append(msg);
			}	
		}
		if(errors.length()>0)throw new IOException(errors.toString());
		return result;
	}

}
