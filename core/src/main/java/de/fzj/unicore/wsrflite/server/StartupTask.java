/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.wsrflite.server;

import java.util.Collection;

/**
 * All implementations of this interface are run at startup of the container. 
 * The order is determined by the name and getAfter and getBefore methods.
 * @author K. Benedyczak
 */
public interface StartupTask {
	
	/**
	 * The main entry point for the startup task code.
	 * @throws Exception If an exception is thrown, the container won't start. The exception will be logged.
	 */
	public void run() throws Exception;
	    
	/**
	 * @return name (id) of the starup task
	 */
	public String getName();
	
	/**
	 * @return names of tasks which should be invoked before this one.
	 */
	public Collection<String> getAfter();
	
	/**
	 * @return names of tasks which should be invoked after this one.
	 */
	public Collection<String> getBefore();
}
