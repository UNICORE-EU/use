package eu.unicore.services.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;


/**
 * runs a task with an external timeout
 *  
 * @param <V> - the type of result
 * 
 * @author schuller
 */
public class TimeoutRunner<V> implements Callable<V> {

	private static final Logger logger = Log.getLogger(Log.UNICORE, TimeoutRunner.class);

	private final Callable<V> task;

	private V result;

	private final int timeout;

	private final TimeUnit unit;

	private final ExecutorService service;

	/**
	 * @param task - the task to execute
	 * @param timeout
	 * @param unit - time unit
	 */
	public TimeoutRunner(Callable<V> task, ExecutorService service, int timeout, TimeUnit unit){
		this.task = task;
		this.timeout = timeout;
		this.unit = unit;
		this.service = service;
	}

	public V call() throws RejectedExecutionException, InterruptedException, ExecutionException {
		logger.debug("Starting task with timeout of {} {}", timeout, unit);
		try{
			result = service.submit(task).get(timeout, unit);
		}
		catch(TimeoutException ignored){
			logger.debug("Timeout reached!");
		}
		return result;
	}

	/**
	 * helper for computing a result using a TimeoutRunner
	 * 
	 * @param <Result>
	 * @param task
	 * @param timeout - time out in milliseconds
	 * @return a Result or <code>null</code> if the timeout is reached
	 * @throws Exception
	 */
	public static <Result> Result compute(Callable<Result> task, ExecutorService service, int timeout)
	throws Exception {
		return compute(task, service, timeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * helper for computing a result using a TimeoutRunner
	 * 
	 * @param <Result>
	 * @param task
	 * @param timeout
	 * @param units the {@link TimeUnit} to use
	 * @return a Result or <code>null</code> if the timeout is reached
	 * @throws Exception
	 */
	public static <Result> Result compute(Callable<Result> task, ExecutorService service,
			int timeout, TimeUnit units) throws Exception {
		return new TimeoutRunner<Result>(task, service, timeout, units).call();
	}

}