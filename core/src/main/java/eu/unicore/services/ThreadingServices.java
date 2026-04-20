package eu.unicore.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

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
		        		return new Thread(r, "use-sched-"+threadNumber.getAndIncrement());
		        	}
				});
	}
	
	protected void configureExecutor(){
		int min = kernelCfg.getIntValue(ContainerProperties.EXEC_CORE_POOL_SIZE);
		final int max = kernelCfg.getIntValue(ContainerProperties.EXEC_MAX_POOL_SIZE);
		int idle = kernelCfg.getIntValue(ContainerProperties.EXEC_POOL_TIMEOUT);
		ConditionalQueue queue = new ConditionalQueue(1024);
		executor = new USEExecutor(min, max, idle, TimeUnit.MILLISECONDS, queue);
		// This custom queue makes the ThreadPoolExecutor scale the way we want,
		// while keeping the core threads alive. The queue will queue the task only if
		// no more workers can be added - otherwise it will reject, which will cause
		// the ThreadPoolExecutor to add a new worker.
		queue.setCondition(()->{
			return executor.getPoolSize()==max;
		});
	}

	public static class ConditionalQueue extends LinkedBlockingQueue<Runnable>{

		private static final long serialVersionUID = 1L;

	    private BooleanSupplier condition = ()->{
	    	return true;
	    };

	    public ConditionalQueue(int capacity, BooleanSupplier condition) {
			super(capacity);
			this.condition = condition;
		}

	    public ConditionalQueue(int capacity) {
			this(capacity, () -> { return false; } );
		}

	    public void setCondition(BooleanSupplier condition) {
	    	this.condition = condition;
	    }

	    @Override
	    public boolean offer(Runnable r) {
	    	return condition.getAsBoolean() ? super.offer(r) : false;
	    }
	}

	public static class USEExecutor extends ThreadPoolExecutor {
		public USEExecutor(int min, int max, long idle, TimeUnit timeUnit, ConditionalQueue queue) {
			super(min, max, idle, timeUnit, queue,
				new ThreadFactory(){
				final AtomicInteger threadNumber = new AtomicInteger(1);
				public Thread newThread(Runnable r) {
					return new Thread(r, "use-exec-"+threadNumber.getAndIncrement());
				}
			});
		}

		@Override
		public synchronized void execute(Runnable r) {
			super.execute(r);
		}
	}
}
