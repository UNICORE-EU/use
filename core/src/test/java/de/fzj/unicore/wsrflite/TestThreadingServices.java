package de.fzj.unicore.wsrflite;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;


public class TestThreadingServices extends TestCase {

	public void test1()throws Exception{
		Callable<String>c=new Callable<String>(){
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

	volatile int running=0;
	
	public void testExecutor()throws InterruptedException{
		running=0;
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		final Set<String> threads=new HashSet<String>();
		for(int i=0;i<3;i++){
			Runnable r=new Runnable(){
				public void run(){
					String tName=Thread.currentThread().getName();
					synchronized(TestThreadingServices.this){
						increment();
						threads.add(tName);
					}
					try{
						Thread.sleep(1000);
					}catch(InterruptedException ie){}
					synchronized(TestThreadingServices.this){
						decrement();
					}
				}
			};
			cp.getThreadingServices().getExecutorService().execute(r);
		}
		Thread.sleep(500);
		while(running>0){
			Thread.sleep(500);
		}
		Thread.sleep(1000);
		assertTrue(threads.size()==3);
	}
	
	public void testScheduler()throws InterruptedException{
		running=0;
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		final Set<String> threads=new HashSet<String>();
		for(int i=0;i<3;i++){
			Runnable r=new Runnable(){
				public void run(){
					String tName=Thread.currentThread().getName();
					synchronized(TestThreadingServices.this){
						increment();
						threads.add(tName);
					}
					try{
						Thread.sleep(1000);
					}catch(InterruptedException ie){}
					synchronized(TestThreadingServices.this){
						decrement();
					}
				}
			};
			cp.getThreadingServices().getScheduledExecutorService().schedule(r, 0, TimeUnit.MILLISECONDS);
		}
		Thread.sleep(500);
		while(running>0){
			Thread.sleep(500);
		}
		Thread.sleep(1000);
		assertTrue(threads.size()==3);
	}
		
	
	synchronized void increment(){
		running++;
	}
	synchronized void decrement(){
		running--;
	}
	
}
