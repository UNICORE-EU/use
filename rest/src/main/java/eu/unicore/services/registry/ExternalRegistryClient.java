package eu.unicore.services.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.services.ExternalSystemConnector.Status;
import eu.unicore.services.rest.client.RESTException;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.services.rest.client.Resources;
import eu.unicore.services.rest.registry.Registries;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * A client for querying and adding to a set of external registries.
 * 
 * @author schuller
 */
public class ExternalRegistryClient implements IRegistry {

	private static final Logger logger = Log.getLogger(Log.CLIENT, ExternalRegistryClient.class);

	protected static final long readd_offset = 60; 

	private final List<RegistryClient> clients = new ArrayList<>();

	public ExternalRegistryClient(){
		super();
	}

	public Calendar addRegistryEntry(Map<String,String> content){
		long refreshIn = doAddRegistryEntry(content);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis() + 1000*refreshIn);
		return c;
	}

	/**
	 * calls "add" on all configured external registries<br>
	 * This method will return the requested time until refresh (in seconds)
	 * returned by any of the external registries, or a default of 5 minutes in case 
	 * no external registry can be contacted.  
	 * 
	 * @return refresh time in seconds
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

	private String statusMessage = "N/A";
	private Status status = Status.DOWN;
	private long lastChecked = 0;

	/**
	 * check the connection to the services. If no service 
	 * replies within the given timeout, returns <code>false</code>
	 */
	private void checkConnection(){
		if(lastChecked+60000>System.currentTimeMillis())
			return;

		final StringBuffer sb = new StringBuffer();
		boolean result = false;
		for(final RegistryClient c: clients){
			Callable<String>task=new Callable<>(){
				public String call()throws Exception{
					try {
						c.getJSON();
						return "OK";
					}catch(RESTException e) {
						return e.getErrorMessage();
					}catch(Exception e) {
						return Log.createFaultMessage("", e);
					}
				}
			};
			String res = compute(task, 5000);
			boolean currentOK = res!=null && "OK".equals(res);
			if(!currentOK){
				sb.append("[").append(c.getURL()+": "+res);
				sb.append("] ");
			}
			result=result || currentOK;
		}
		if(result) {
			status = Status.OK;
			statusMessage = "OK";
		}
		else {
			status = Status.DOWN;
			statusMessage = sb.toString();
		}
		lastChecked = System.currentTimeMillis();
	}

	public Status getConnectionStatus(){
		checkConnection();
		return status;		
	}

	public String getConnectionStatusMessage(){
		checkConnection();
		return statusMessage;	
	}

	private String compute(Callable<String>task, int timeout){
		try{
			Future<String> f = Resources.getExecutorService().submit(task);
			return f.get(timeout, TimeUnit.MILLISECONDS);
		}catch(Exception ex){
			return "ERROR";
		}
	}

	/**
	 * get a client for talking to the external registries
	 * @return {@link ExternalRegistryClient}
	 */ 
	public static ExternalRegistryClient getExternalRegistryClient(Collection<String> externalRegistryURLs, ClientProperties sec)throws Exception{
		ExternalRegistryClient reg = new ExternalRegistryClient();
		synchronized(externalRegistryURLs){
			for(String url: externalRegistryURLs){
				try{
					if(!url.contains("/rest/registries/")) {
						url = Registries.convertToREST(url);
					}
					reg.clients.add(new RegistryClient(url, sec));
				}catch(Exception e){
					logger.error("Could not create client for external registry at <"+url+">",e);
				}
			}
		}
		return reg;
	}

}
