package eu.unicore.services.utils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;

/**
 * helper class to perform an update on a resource
 *
 * @author schuller
 */
public abstract class AsyncCallback<T extends Resource> implements Runnable{

	private final Home home;

	private final String resourceID;

	private final int delay;

	private Exception exception;

	public AsyncCallback(Home home, String resourceID, int delay){
		this.home=home;
		this.resourceID=resourceID;
		this.delay = delay;
	}

	public AsyncCallback(Home home, String resourceID){
		this(home, resourceID, 200);
	}

	@SuppressWarnings("unchecked")
	public void run(){
		try(T resource = (T)home.getForUpdate(resourceID)){
			callback(resource);
		}
		catch(Exception ex){
			exception = ex;
		}
	}

	/**
	 * perform callback
	 * @param resource - the resource
	 */
	public abstract void callback(T resource);

	public ScheduledFuture<?> submit() {
		return home.getKernel().getContainerProperties().getThreadingServices().
		getScheduledExecutorService().schedule(this, delay, TimeUnit.MILLISECONDS);
	}

	public Exception getException() {
		return exception;
	}
}
