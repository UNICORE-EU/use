/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services;

import eu.unicore.services.server.AbstractStartupTask;

public class ExampleStartupTask extends AbstractStartupTask {
	public static int runCount = 0;
	
	@Override
	public void run() {
		runCount++;
	}
}
