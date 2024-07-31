package eu.unicore.services.utils;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

public class TestMetricUtils {
	
	@Test
	public void testGetMetricValues(){
		System.out.println("Gauge : "+MetricUtils.getValue(new Gauge<String>() {
			public String getValue(){
				return "test123";
			}
		}));

		Timer t = new Timer();
		t.update(1234, TimeUnit.MILLISECONDS);
		System.out.println("Timer: "+MetricUtils.getValue(t));
		
		Meter m = new Meter();
		m.mark(123);
		System.out.println("Meter: "+MetricUtils.getValue(m));
		
		Counter c = new Counter();
		c.inc(123);
		System.out.println("Counter: "+MetricUtils.getValue(c));
		
	}

}
