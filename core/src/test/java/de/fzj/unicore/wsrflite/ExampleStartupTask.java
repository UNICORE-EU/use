/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.wsrflite;

import de.fzj.unicore.wsrflite.server.AbstractStartupTask;

public class ExampleStartupTask extends AbstractStartupTask {
	public static int runCount = 0;
	
	@Override
	public void run() {
		runCount++;
	}
}
