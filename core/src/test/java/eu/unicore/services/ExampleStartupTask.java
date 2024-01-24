package eu.unicore.services;

import eu.unicore.services.server.AbstractStartupTask;

public class ExampleStartupTask extends AbstractStartupTask {
	public static int runCount = 0;
	
	@Override
	public void run() {
		runCount++;
	}
}
