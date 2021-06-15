package eu.unicore.services.utils;

import com.codahale.metrics.Gauge;

/**
 * The "circuit breaker" is described in the book "Release it!" by Michael Nygard.
 * It is used to avoid making repeated calls to an external service that is 
 * unavailable. 
 * 
 * The circuit breaker has two states: "OK" and "not OK". Once switched to "not OK" it 
 * will stay that way for a predefined period of time (say 1 minute), or until it
 * is explicitely switched back on (i.e. to "OK"). 
 * 
 * Code that makes calls to external services should have an instance of this class for
 * each external service that it talks to. Failure to reach the service then sets the
 * circuit breaker to "not OK". Before each attempt to call the external service, the 
 * code can check the state of the circuit breaker and avoid making the failing 
 * call (again).
 * 
 * After the configured waiting period, the circuit breaker will reset itself, and the call
 * can be attempted again.
 * 
 * The current state of the circuit breaker is available via the metrics system of the Kernel.
 * 
 * @author schuller
 */
public class CircuitBreaker implements Gauge<String> {

	private boolean ok = true;

	private long disabledAt = 0;

	// waiting period in milliseconds
	private long waitingPeriod;

	// the name of this CB - should correlate to the external service being used
	private String name;
	
	private String errorMessage;
	
	/**
	 * create a new CircuitBreaker with the default waiting period of 60 seconds
	 * @param name
	 */
	public CircuitBreaker(String name){
		this(name, 60*1000);
	}

	/**
	 * create a new CircuitBreaker with the specified waitingPeriod
	 * @param waitingPeriod - time interval in milliseconds that the CB stays in "not OK" mode
	 */
	public CircuitBreaker(String name, long waitingPeriod){
		this.waitingPeriod = waitingPeriod;
		this.name = name;
	}

	/**
	 * Check the state of the circuit breaker. If "not OK", it will check whether the 
	 * waiting period has passed. If yes the circuit will be re-enabled.
	 * 
	 * @return <code>true</code> if the circuit breaker is OK
	 */
	public synchronized boolean isOK(){
		if(!ok){
			// check if waiting period has passed, and if yes
			// reset the state to "ok"
			if(disabledAt+waitingPeriod<System.currentTimeMillis()){
				ok = true;
			}
		}
		return ok;
	}

	/**
	 * set the circuit breaker to "not OK" mode. This mode will
	 * be active at least for the waiting period, or until manually reset.
	 */
	public void notOK(){
		notOK(null);
	}
	
	public synchronized void notOK(String errorMessage){
		ok = false;
		disabledAt = System.currentTimeMillis();
		this.errorMessage = errorMessage;
	}

	/**
	 * reset the circuit breaker to "OK" mode
	 */
	public synchronized void OK(){
		ok = true;
	}

	public String getName() {
		return "use.externalConnectionStatus."+name;
	}

	@Override
	public String getValue() {
		return doGetValue();
	}

	private String doGetValue(){
		return isOK() ? "OK": ("not OK"+(errorMessage!=null?": "+errorMessage : ""));
	}
}
