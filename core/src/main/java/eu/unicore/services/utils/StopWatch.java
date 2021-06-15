package eu.unicore.services.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * little helper for timed tests, performance test runs, etc.
 * 
 * @author schuller
 */
public class StopWatch {

	private long start;
	
	private long end;
	
	private long lap_start;
	
	private final List<String>messages;
	
	private boolean stopped=false;
	
	private boolean echo_messages = true;
	
	public StopWatch() {
		super();
		messages=new ArrayList<String>();
	}

	public void start(String message){
		start=System.currentTimeMillis();
		lap_start = start;
		messages.add("["+Calendar.getInstance().getTime()+"] "+message);
	}
	
	public void snapShot(String description){
		long instant = System.currentTimeMillis();
		String msg = "["+(instant-start)+"]["+(instant-lap_start)+"]"+description;
		lap_start = System.currentTimeMillis();
		messages.add(msg);
		if(echo_messages)System.out.println(msg);
	}
	
	public long stop(String message){
		snapShot(message);
		messages.add("["+Calendar.getInstance().getTime()+"] End of measurement.");
		stopped=true;
		end=System.currentTimeMillis();
		return end-start;
	}
	
	public long getCurrentTotal(){
		if(!stopped){
			return System.currentTimeMillis()-start;
		}
		return end-start;
	}
	
	public String toString(){
		StringBuffer sb=new StringBuffer();
		for(String m:messages)sb.append(m+"\n");
		return sb.toString();
	}
	
	public void setEchoMessages(boolean echo){
		this.echo_messages = echo;
	}
}
