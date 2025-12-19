package eu.unicore.services.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;

/**
 * for tracking time use / debugging purposes
 * @author schuller
 */
public class TimeProfile {

	private static Logger logger = Log.getLogger(Log.ADMIN, TimeProfile.class);

	private final String name;
	private final long start;
	private final List<String>timestamps = new ArrayList<>();

	public TimeProfile(String name) {
		this.name = name;
		this.start = System.currentTimeMillis();
	}

	public void time(String description) {
		timestamps.add((System.currentTimeMillis()-start)+": "+description);
	}

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		sb.append(name).append("::");
		for(String m:timestamps)sb.append(" ").append(m);
		return sb.toString();
	}
	
	public void log() {
		if(logger.isDebugEnabled() && timestamps.size()>0) {
			logger.debug(toString());
		}
	}

	public boolean isEnabled() {
		return logger.isDebugEnabled();
	}

}