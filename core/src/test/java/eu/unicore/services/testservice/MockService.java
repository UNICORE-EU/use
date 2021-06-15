package eu.unicore.services.testservice;

import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.Service;

public class MockService implements Service{

	public static final String TYPE="mock";
	
	private final String name;

	private volatile boolean stopped=true;
	
	private final AtomicInteger invokeCount=new AtomicInteger(0);
	
	private final Home home;
	
	private final Kernel kernel;
	
	public MockService(String name, Kernel kernel){
		this.name=name;
		this.kernel=kernel;
		this.home=new MockHome();
	}
	
	public int getInvocationCount(){
		return invokeCount.get();
	}
	
	public void clearInvocationCount(){
		invokeCount.set(0);
	}
	
	public String getName() {
		return name;
	}

	public String getType() {
		return MockService.TYPE;
	}

	public void invoke(){
		if(stopped)throw new IllegalStateException("Service stopped.");
		invokeCount.incrementAndGet();
	}

	public void start() throws Exception {
		System.out.println("Started "+this);
		stopped=false;
		if(home!=null){
			home.setKernel(kernel);
			home.activateHome(name);
		}
	}

	public void stop() {
		stopped=true;
		if(home!=null){
			home.passivateHome();
		}
	}

	public void stopAndCleanup() {
		stop();
	}

	public Home getHome(){
		return home;
	}
	
	public boolean isStarted(){
		return !stopped;
	}
	
	public String getInterfaceClass(){
		return IMock.class.getName();
	}
}
