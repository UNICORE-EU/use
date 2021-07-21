/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.StartupTask;
import eu.unicore.util.Log;

/**
 * Handles running of startup tasks.
 * @author K. Benedyczak
 */
public class StartupTasksRunner {
	private static final Logger logger = Log.getLogger(Log.UNICORE, StartupTasksRunner.class);
	
	
	
	public void runStartupTasks(Kernel kernel, Iterable<StartupTask> sl) throws Exception {
		Map<String, StartupTask> done = new HashMap<String, StartupTask>();
		Map<String, StartupTask> todo = new HashMap<String, StartupTask>();
		for (StartupTask task: sl) {
			if (todo.containsKey(task.getName()))
				throw new IllegalStateException("Problem with starting init tasks. " +
						"The task with name " + task.getName() + " is configured twice.");
			todo.put(task.getName(), task);
		}
		
		int doneNum = 0;
		while (todo.size() > 0) {
			for (StartupTask candidate: todo.values()) {
				if (isEligibleToRun(candidate, done, todo)) {
					logger.info("Running startup task <{}>", candidate.getName());
					if (candidate instanceof KernelInjectable) {
						((KernelInjectable)candidate).setKernel(kernel);
					}
					candidate.run();
					todo.remove(candidate.getName());
					done.put(candidate.getName(), candidate);
					break;
				}
			}
			if (done.size() == doneNum)
				throw new IllegalStateException("Problem with starting init tasks. " +
						"The dependencies between tasks can't be satisfied. " +
						"There are the following not invoked tasks: " + todo.keySet() + 
						" and the following were run: " + done.keySet());
			doneNum++;
		}
		
	}
	
	private boolean isEligibleToRun(StartupTask candidate, Map<String, StartupTask> done, 
			Map<String, StartupTask> todo) {
		//1: all from getAfter are done
		for (String after: candidate.getAfter())
			if (!done.containsKey(after))
				return false;
		//2: there is no todo task which must be run before this one
		for (StartupTask other: todo.values()) {
			if (other.equals(candidate))
				continue;
			if (other.getBefore().contains(candidate.getName()))
				return false;
		}
		return true;
	}
	

}
