package eu.unicore.services.ws.client;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resource pool providing centralized thread/execution management for Client use<br>
 * 
 * TODO configuration
 * 
 * @author schuller
 */
public class Resources {
	
	/**
	 * property key for setting the core thread pool size for the 
	 * scheduled execution service
	 */
	public static final String CORE_POOL_SIZE="wsrflite.resources.scheduled.size";
	
	/**
	 * property key for setting the timeout in millis for removing idle threads
	 */
	public static final String POOL_TIMEOUT="wsrflite.resources.scheduled.idletime";
	
	/**
	 * property key for setting the minimum thread pool size for the 
	 * scheduled execution service
	 */
	public static final String EXEC_CORE_POOL_SIZE="wsrflite.resources.executor.minsize";
	
	/**
	 * property key for setting the maximum thread pool size for the 
	 * scheduled execution service
	 */
	public static final String EXEC_MAX_POOL_SIZE="wsrflite.resources.executor.maxsize";
	
	/**
	 * property key for setting the timeout in millis for removing idle threads
	 */
	public static final String EXEC_POOL_TIMEOUT="wsrflite.resources.executor.idletime";
	
	private Resources(){}
	
	private static boolean isConfigured=false;
	
	private static ScheduledThreadPoolExecutor scheduler;
	
	private static ThreadPoolExecutor executor;
	
	/**
	 * get a {@link ScheduledExecutorService} for executing tasks at a given schedule
	 */
	public static synchronized ScheduledExecutorService getScheduledExecutorService(){
		if(!isConfigured)configure();
		return scheduler;
	}
	
	/**
	 * get a {@link ExecutorService} for executing tasks
	 * @return ExecutorService
	 */
	public static synchronized ExecutorService getExecutorService(){
		if(!isConfigured)configure();
		return executor;
	}
	
	/**
	 * get an {@link CompletionService} using the Exector service
	 * @param <V>
	 */
	public static synchronized <V> CompletionService<V>getCompletionService(){
		return new ExecutorCompletionService<V>(getExecutorService());
	}
	
	/**
	 * Configure the pool
	 */
	protected static void configure(){
		configureScheduler();
		configureExecutor();
		isConfigured=true;
	}
	
	protected static void configureScheduler(){
		int core=2;
		scheduler=new ScheduledThreadPoolExecutor(core);
		int idle=50;
		scheduler.setKeepAliveTime(idle, TimeUnit.MILLISECONDS);
		scheduler.setThreadFactory(new ThreadFactory(){
        			final AtomicInteger threadNumber = new AtomicInteger(1);
		        	public Thread newThread(Runnable r) {
		        		Thread t = new Thread(r);
		        		t.setName("client-sched-"+threadNumber.getAndIncrement());
		        		return t;
		        	}
				});
	}
	
	protected static void configureExecutor(){
		int min=2;
		int max=32;
		int idle=50;
		
		executor=new ThreadPoolExecutor(min,max,
				idle,TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new ThreadFactory(){
        			final AtomicInteger threadNumber = new AtomicInteger(1);
		        	public Thread newThread(Runnable r) {
		        		Thread t = new Thread(r);
		        		t.setName("wsrflite-executor-"+threadNumber.getAndIncrement());
		        		return t;
		        	}
				});

	}
	
	/**
	 * get the current minimum pool size of the scheduler pool
	 */
	public static int getScheduledExecutorCorePoolSize(){
		return scheduler.getCorePoolSize();
	}
	
	/**
	 * get the current maximum pool size of the scheduler pool
	 */
	public static int getScheduledExecutorMaxPoolSize(){
		return scheduler.getMaximumPoolSize();
	}
	
	/**
	 * get the number of currently active threads in the scheduler pool
	 */
	public static int getScheduledExecutorActiveThreadCount(){
		return scheduler.getActiveCount();
	}
	
	public static int getExecutorCorePoolSize(){
		return executor.getCorePoolSize();
	}
	
	public static int getExecutorMaxPoolSize(){
		return executor.getMaximumPoolSize();
	}
	
	public static int getExecutorActiveThreadCount(){
		return executor.getActiveCount();
	}

}

