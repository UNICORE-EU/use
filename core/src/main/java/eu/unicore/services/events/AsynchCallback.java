package eu.unicore.services.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;

/**
 * helper class to perform an update on a resource
 *
 * @author schuller
 */
public class AsynchCallback<T extends Resource> implements Runnable{
	
	protected final static Logger log=Log.getLogger(Log.UNICORE, AsynchCallback.class);
	
	private final Home home;
	private final String resourceID;

	private final int delay;

	public AsynchCallback(Home home, String resourceID, int delay){
		this.home=home;
		this.resourceID=resourceID;
		this.delay = 200;
	}
	
	public AsynchCallback(Home home, String resourceID){
		this(home, resourceID, 200);
	}

	@SuppressWarnings("unchecked")
	public void run(){
		try(T resource = (T)home.getForUpdate(resourceID)){
			callback(resource);
		}
		catch(Exception ex){
			Log.logException("Error", ex, log);
		}
	}

	/**
	 * perform callback
	 * @param resource - the resource
	 */
	public void callback(T resource) {
		
	}

	public void submit() {
		home.getKernel().getContainerProperties().getThreadingServices().
		getScheduledExecutorService().schedule(this, delay, TimeUnit.MILLISECONDS);
	}
}
