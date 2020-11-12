package de.fzj.unicore.wsrflite.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.ThreadingServices;
import eu.unicore.util.Log;


/**
 * runs a task with an external timeout
 *  
 * @param <V> - the type of result
 * 
 * @author schuller
 */
public class TimeoutRunner<V> implements Callable<V> {

	private static final Logger logger=Log.getLogger(Log.UNICORE,TimeoutRunner.class);

	private final Callable<V> task;
	
	private V result;
	
	private final int timeout;
	
	private final TimeUnit unit;
	
	private final ThreadingServices service;
	
	/**
	 * @param timeout - milliseconds before timeout
	 * @param task - the task to execute
	 */
	public TimeoutRunner(Callable<V> task, ThreadingServices service, int timeout, TimeUnit unit){
		this.task=task;
		this.timeout=timeout;
		this.unit=unit;
		this.service = service;
	}
	
	public V call() throws RejectedExecutionException, InterruptedException, ExecutionException {
		if(logger.isDebugEnabled())logger.debug("Starting task with timeout of "+timeout+ " "+unit);
		try{
			Future<V> res=service.getExecutorService().submit(task);
			result=res.get(timeout, unit);
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
	 * @return a Result or <code>null</code> if the timeout is reached, or an exception occurs
	 */
	public static <Result> Result compute(Callable<Result> task, ThreadingServices service, int timeout){
		return compute(task, service, timeout, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * helper for computing a result using a TimeoutRunner
	 * 
	 * @param <Result>
	 * @param task
	 * @param timeout
	 * @param units the {@link TimeUnit} to use
	 * @return a Result or <code>null</code> if the timeout is reached or an exception occurs
	 */
	public static <Result> Result compute(Callable<Result> task, ThreadingServices service, int timeout, TimeUnit units){
		TimeoutRunner<Result> runner=new TimeoutRunner<Result>(task, service, timeout, units);
		try{
			return runner.call();
		}catch(Exception e){
			return null;
		}
	}
	
}
