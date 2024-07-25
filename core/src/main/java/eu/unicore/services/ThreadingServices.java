package eu.unicore.services;

import java.util.concurrent.BlockingQueue;
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
 * Resource pool providing centralized thread/execution management
 * Offers scheduled execution, and an execution queue.
 * <code>
 *  Runnable task=...;
 *  ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
 *  ts.getExecutorService().execute(task);
 * </code>
 * 
 * @see ExecutorService
 * @see ScheduledExecutorService
 * 
 * @author schuller
 * @since 2.2.0
 */
public class ThreadingServices {

	private final ContainerProperties kernelCfg;
	
	private ScheduledThreadPoolExecutor scheduler;
	
	private ThreadPoolExecutor executor;
	
	
	/**
	 * This class instance usually should be an application-level singleton,
	 * i.e. the code should get an instance of this class via {@link ContainerProperties}
	 * @param kernelCfg
	 */
	protected ThreadingServices(ContainerProperties kernelCfg){
		this.kernelCfg=kernelCfg;
		configure();
	}
	
	/**
	 * get a {@link ScheduledExecutorService} for executing tasks at a given schedule
	 */
	public synchronized ScheduledExecutorService getScheduledExecutorService(){
		return scheduler;
	}
	
	/**
	 * get a {@link ExecutorService} for executing tasks
	 * @return ExecutorService
	 */
	public ExecutorService getExecutorService(){
		return executor;
	}
	
	/**
	 * get an {@link CompletionService} using the Exector service
	 * @param <V>
	 */
	public synchronized <V> CompletionService<V>getCompletionService(){
		return new ExecutorCompletionService<V>(getExecutorService());
	}
	
	/**
	 * Configure the pool. Properties are read from
	 * the {@link Kernel} properties.
	 */
	protected void configure(){
		configureScheduler();
		configureExecutor();
	}
	
	protected void configureScheduler(){
		int core=kernelCfg.getIntValue(ContainerProperties.CORE_POOL_SIZE);
		scheduler=new ScheduledThreadPoolExecutor(core);
		int idle=kernelCfg.getIntValue(ContainerProperties.POOL_TIMEOUT);
		scheduler.setKeepAliveTime(idle, TimeUnit.MILLISECONDS);
		scheduler.setThreadFactory(new ThreadFactory(){
        			final AtomicInteger threadNumber = new AtomicInteger(1);
		        	public Thread newThread(Runnable r) {
		        		Thread t = new Thread(r);
		        		t.setName("use-sched-"+threadNumber.getAndIncrement());
		        		return t;
		        	}
				});
	}
	
	protected void configureExecutor(){
		int min = kernelCfg.getIntValue(ContainerProperties.EXEC_CORE_POOL_SIZE);
		int max = kernelCfg.getIntValue(ContainerProperties.EXEC_MAX_POOL_SIZE);
		int idle = kernelCfg.getIntValue(ContainerProperties.EXEC_POOL_TIMEOUT);

		executor = new UseExecutor(min, max,
				idle, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(4*max),
				new ThreadFactory(){
        			final AtomicInteger threadNumber = new AtomicInteger(1);
		        	public Thread newThread(Runnable r) {
		        		Thread t = new Thread(r);
		        		t.setName("use-exec-"+threadNumber.getAndIncrement());
		        		return t;
		        	}
				});
	}

	/**
	 * get the current minimum pool size of the scheduler pool
	 */
	public int getScheduledExecutorCorePoolSize(){
		return scheduler.getCorePoolSize();
	}

	/**
	 * get the current maximum pool size of the scheduler pool
	 */
	public int getScheduledExecutorMaxPoolSize(){
		return scheduler.getMaximumPoolSize();
	}

	/**
	 * get the number of currently active threads in the scheduler pool
	 */
	public int getScheduledExecutorActiveThreadCount(){
		return scheduler.getActiveCount();
	}

	public int getExecutorCorePoolSize(){
		return executor.getCorePoolSize();
	}

	public int getExecutorMaxPoolSize(){
		return executor.getMaximumPoolSize();
	}

	public int getExecutorActiveThreadCount(){
		return executor.getActiveCount();
	}

	/**
	 * Improves the behaviour of the ThreadPoolExecutor:
	 * in case there are less than max threads running,
	 * more threads are be started before filling up the queue
	 *
	 * @author schuller
	 */
	public static class UseExecutor extends ThreadPoolExecutor {

		private int coreSize;

		public UseExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
				BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
			this.coreSize = corePoolSize;
		}

		@Override
		public void execute(Runnable command) {
		    super.execute(command);
		    final int poolSize = getPoolSize();
		    if (poolSize < getMaximumPoolSize()) {
		        if (getQueue().size() > 0) {
		            synchronized (this) {
		                setCorePoolSize(poolSize + 1);
		                setCorePoolSize(coreSize);
		            }
		        }
		    }
		}
	}

}

