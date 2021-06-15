package eu.unicore.services.events;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;

/**
 * helper class to perform some asynchronous operation and notify a {@link Resource} after the
 * task was finished
 *
 * @author schuller
 */
public abstract class AsynchActionWithCallback<T extends Resource> implements Runnable{

	protected final static Logger log=Log.getLogger(Log.UNICORE, AsynchActionWithCallback.class);
	
	private final Runnable task;
	private final Home home;
	private final String resourceID;

	public AsynchActionWithCallback(Runnable task, Home home, String resourceID){
		this.task=task;
		this.home=home;
		this.resourceID=resourceID;
	}

	/**
	 * Executes the task. If successful (i.e. no exception is thrown in task.run()), the
	 * correct {@link Resource} is updated via the {@link #taskFinished(Resource)} method 
	 */
	@SuppressWarnings("unchecked")
	public void run(){
		RuntimeException re=null;
		try{
			task.run();
		}catch(RuntimeException ex){
			re=ex;
		}
		T resource=null;
		try{
			resource=(T)home.getForUpdate(resourceID);
			if(re==null){
				taskFinished(resource);
			}
			else{
				taskFailed(resource,re);
			}
		}
		catch(Exception ex){
			Log.logException("Error", ex, log);
		}
		finally{
			if(resource!=null){
				try{
					home.persist(resource);
				}catch(Exception ex){
					Log.logException("Error persisting", ex, log);
				}
			}
		}
	}

	/**
	 * called after the task has finished successfully
	 * @param resource - the resource
	 */
	public abstract void taskFinished(T resource);

	/**
	 * called after the task has finished with an exception
	 * @param resource - the resource
	 * @param error - the RuntimeException that was thrown by the task
	 */
	public abstract void taskFailed(T resource, RuntimeException error);

}
