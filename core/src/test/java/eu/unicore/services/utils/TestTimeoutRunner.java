package eu.unicore.services.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ThreadingServices;

public class TestTimeoutRunner {
	
	protected ThreadingServices ts;
	
	@Before
	public void setUp() {
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		ts = cp.getThreadingServices();		
	}

	Callable<String>task1 = new Callable<>(){
		public String call() throws Exception{
			for(int t=0;t<10;t++){
				Thread.sleep(100);
			}
			return "Result";
		}
	};

	Callable<String>task2 = new Callable<>(){
		public String call() throws Exception{
			for(int t=0;t<3;t++){
				Thread.sleep(100);
			}
			return "Result";
		}
	};

	@Test
	public void testTaskWithTimeout(){
		//call with short timeout
		String result=TimeoutRunner.compute(task1, ts, 100);
		assertNull(result);
	}
	
	@Test
	public void testTaskWithoutTimeout(){
		//call with long timeout
		String result=TimeoutRunner.compute(task1, ts, 2000);
		assertNotNull(result);
		assertEquals("Result",result);
	}

	@Test
	public void testAccuracyOfTimeout(){
		//call with short timeout
		String result=TimeoutRunner.compute(task2, ts, 50);
		assertNull(result);
		//call with long enough timeout
		result=TimeoutRunner.compute(task2, ts, 5000);
		assertNotNull(result);
	}

	@Test
	public void testTaskWithTimeoutUsingUnits(){
		//call with short timeout
		String result=TimeoutRunner.compute(task1, ts, 1000, TimeUnit.MICROSECONDS);
		assertNull(result);
	}

	@Test
	public void testTaskWithoutTimeoutUsingUnits(){
		//call with long timeout
		String result=TimeoutRunner.compute(task1, ts, 10, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("Result",result);
	}

}
