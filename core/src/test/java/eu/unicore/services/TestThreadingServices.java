package eu.unicore.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;


public class TestThreadingServices {

	@Test
	public void test1()throws Exception{
		Callable<String>c = new Callable<>(){
			public String call()throws Exception{
				Thread.sleep(2000);
				return "OK";
			}
		};
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		CompletionService<String>cs=cp.getThreadingServices().getCompletionService();
		cs.submit(c);
		Future<String> f=cs.take();
		assertEquals("OK",f.get());
		
		//test with timeout
		cs.submit(c);
		Future<String>r1=cs.poll(1, TimeUnit.MILLISECONDS);
		assertNull(r1, "Should timeout!");
	}

	AtomicInteger running = new AtomicInteger(0);

	@Test
	public void testExecutor()throws InterruptedException{
		running.set(0);
		Properties props = new Properties();
		props.setProperty("container."+ContainerProperties.EXEC_POOL_TIMEOUT, "3000");
		ContainerProperties cp = new ContainerProperties(props, false);
		final Set<String> threads = ConcurrentHashMap.newKeySet();
		int poolSize = cp.getIntValue(ContainerProperties.EXEC_MAX_POOL_SIZE);
		for(int i=0;i<2*poolSize;i++){
			Runnable r=new Runnable(){
				public void run(){
					String tName=Thread.currentThread().getName();
					running.incrementAndGet();
					threads.add(tName);
					try{
						Thread.sleep(1000);
					}catch(InterruptedException ie){}
					running.decrementAndGet();
				}
			};
			cp.getThreadingServices().getExecutorService().execute(r);
		}
		while(running.get()>0){
			Thread.sleep(500);
		}
		assertTrue(threads.size()<=poolSize);
		System.out.println("Threads used:   "+threads.size());
		System.out.println("Active threads: "+cp.getThreadingServices().getExecutorActiveThreadCount());
		ThreadPoolExecutor ex = (ThreadPoolExecutor)cp.getThreadingServices().getExecutorService();
		System.out.println("Core threads:   "+ex.getCorePoolSize());
	}

	@Test
	public void testScheduler()throws InterruptedException{
		running.set(0);
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		final Set<String> threads = ConcurrentHashMap.newKeySet();
		for(int i=0;i<3;i++){
			Runnable r=new Runnable(){
				public void run(){
					String tName=Thread.currentThread().getName();
					running.incrementAndGet();
					threads.add(tName);
					try{
						Thread.sleep(1000);
					}catch(InterruptedException ie){}
					running.decrementAndGet();
				}
			};
			cp.getThreadingServices().getScheduledExecutorService().schedule(r, 0, TimeUnit.MILLISECONDS);
		}
		Thread.sleep(500);
		while(running.get()>0){
			Thread.sleep(500);
		}
		Thread.sleep(1000);
		assertTrue(threads.size()==cp.getIntValue(ContainerProperties.CORE_POOL_SIZE));
	}

}
