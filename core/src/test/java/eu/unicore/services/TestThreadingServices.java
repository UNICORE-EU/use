package eu.unicore.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;


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
		assertNull("Should timeout",r1);
	}

	AtomicInteger running = new AtomicInteger(0);

	@Test
	public void testExecutor()throws InterruptedException{
		running.set(0);
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		final Set<String> threads = ConcurrentHashMap.newKeySet();
		int poolSize = cp.getIntValue(ContainerProperties.EXEC_CORE_POOL_SIZE);
		for(int i=0;i<poolSize+1;i++){
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
		Thread.sleep(500);
		while(running.get()>0){
			Thread.sleep(500);
		}
		Thread.sleep(1000);
		assertTrue("Have "+threads.size(), threads.size()==poolSize);
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
		assertTrue("Have "+threads.size(), threads.size()==cp.getIntValue(ContainerProperties.CORE_POOL_SIZE));
	}

}
