package eu.unicore.services.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

public class MetricUtils {

	private static double durationFactor = 1.0/TimeUnit.MILLISECONDS.toNanos(1);

	public static Map<String,String> getValues(MetricRegistry registry){
		return getValues(registry.getMetrics());
	}

	public static Map<String,String> getValues(Map<String,Metric> metrics){
		Map<String,String> res = new HashMap<>();
		for(Map.Entry<String,Metric>e: metrics.entrySet()){
			res.put(e.getKey(), getValue(e.getValue()));
		}
		return res;
	}

	public static String getValue(Metric m){
		if(m instanceof Gauge)return reportGauge((Gauge<?>)m);
		else if(m instanceof Counter)return reportCounter((Counter)m);
		else if(m instanceof Timer)return reportTimer((Timer)m);
		else if(m instanceof Meter)return reportMeter((Meter)m);
		else return "n/a";
	}

    protected double convertDuration(double duration) {
        return duration * durationFactor;
    }
   
	private static String reportTimer(Timer timer) {
		final Snapshot snapshot = timer.getSnapshot();

		return String.format("count=%d,max=%.2f,mean=%.2f,min=%.2f,mean_rate=%.2f,m1_rate=%.2f,m5_rate=%.2f,m15_rate=%.2f",
				timer.getCount(),
				durationFactor*snapshot.getMax(),
				durationFactor*snapshot.getMean(),
				durationFactor*snapshot.getMin(),
				timer.getMeanRate(),
				timer.getOneMinuteRate(),
				timer.getFiveMinuteRate(),
				timer.getFifteenMinuteRate()
				);
    }

    private static String reportMeter(Meter meter) {
    	return String.format("count=%d,mean=%.2f,m1_rate=%.2f,m5_rate=%.2f,m15_rate=%.2f",
    	       meter.getCount(),
               meter.getMeanRate(),
               meter.getOneMinuteRate(),
               meter.getFiveMinuteRate(),
               meter.getFifteenMinuteRate()
               );
    }

    private static String reportCounter(Counter counter) {
        return String.format("count=%d", counter.getCount());
    }

    private static String reportGauge(Gauge<?> gauge) {
        return String.valueOf(gauge.getValue());
    }

}