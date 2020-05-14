package de.fzj.unicore.wsrflite.utils;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.ThreadingServices;
import junit.framework.TestCase;

public class TestTimeoutRunner extends TestCase {
	
	protected ThreadingServices ts;
	
	@Override
	protected void setUp() {
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		ts = cp.getThreadingServices();		
	}
	
	Callable<String>task1=new Callable<String>(){
		public String call() throws Exception{
			for(int t=0;t<10;t++){
				Thread.sleep(100);
			}
			return "Result";
		}
	};
	
	Callable<String>task2=new Callable<String>(){
		public String call() throws Exception{
			for(int t=0;t<3;t++){
				Thread.sleep(100);
			}
			return "Result";
		}
	};
	
	public void testTaskWithTimeout(){
		//call with short timeout
		String result=TimeoutRunner.compute(task1, ts, 100);
		assertNull(result);
	}
	
	
	public void testTaskWithoutTimeout(){
		//call with long timeout
		String result=TimeoutRunner.compute(task1, ts, 2000);
		assertNotNull(result);
		assertEquals("Result",result);
	}

	public void testAccuracyOfTimeout(){
		//call with short timeout
		String result=TimeoutRunner.compute(task2, ts, 50);
		assertNull(result);
		//call with long enough timeout
		result=TimeoutRunner.compute(task2, ts, 5000);
		assertNotNull(result);
	}

	public void testTaskWithTimeoutUsingUnits(){
		//call with short timeout
		String result=TimeoutRunner.compute(task1, ts, 1000, TimeUnit.MICROSECONDS);
		assertNull(result);
	}
	
	
	public void testTaskWithoutTimeoutUsingUnits(){
		//call with long timeout
		String result=TimeoutRunner.compute(task1, ts, 10, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("Result",result);
	}

}
