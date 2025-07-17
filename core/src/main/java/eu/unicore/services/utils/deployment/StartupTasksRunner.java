package eu.unicore.services.utils.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.StartupTask;
import eu.unicore.util.Log;

/**
 * Handles running of startup tasks.
 *
 * @author K. Benedyczak
 * @author schuller
 */
public class StartupTasksRunner implements Runnable {

	private static final Logger logger = Log.getLogger(Log.UNICORE, StartupTasksRunner.class);

	private final Kernel kernel;

	private final List<StartupTask> tasks;

	public StartupTasksRunner(Kernel kernel, List <StartupTask> tasks) {
		this.kernel = kernel;
		this.tasks = tasks;
	}

	public void run() {		
		for (StartupTask candidate: getOrderedTasks()) {
			logger.info("Running startup task <{}>", candidate.getName());
			if (candidate instanceof KernelInjectable) {
				((KernelInjectable)candidate).setKernel(kernel);
			}
			candidate.run();
		}
	}

	List<StartupTask> getOrderedTasks() {
		Map<String, StartupTask> taskMap = new HashMap<>();
		// key: task name, value: list of dependent tasks
		Map<String, List<String>> dependentTasks = new HashMap<>();
		// key: task name, value: number of remaining dependencies
		Map<String, Integer> depCounts = new HashMap<>();
		for (StartupTask task: tasks) {
			taskMap.put(task.getName(), task);
			dependentTasks.put(task.getName(), new ArrayList<>());
			depCounts.put(task.getName(), Integer.valueOf(0));
		}
		// build dependencies
		for (StartupTask task: tasks) {
			Collection<String> before = task.getBefore();
			Collection<String> after = task.getAfter();
			for(String b: before) {
				if(taskMap.containsKey(b)) {
					dependentTasks.get(task.getName()).add(b);
					depCounts.put(b, depCounts.get(b)+1);
				}
			}
			for(String a: after) {
				if(taskMap.containsKey(a)) {
					dependentTasks.get(a).add(task.getName());
					depCounts.put(task.getName(), depCounts.get(task.getName())+1);
				}
			}
		}
		Queue<StartupTask> queue = new LinkedList<>();
		// start with tasks with no deps
        for (StartupTask task : tasks) {
            if (depCounts.get(task.getName()) == 0) {
                queue.offer(task);
            }
        }       
		List<StartupTask>orderedTasks = new ArrayList<>();
        while (!queue.isEmpty()) {
            StartupTask current = queue.poll();
            orderedTasks.add(current);
            for (String waiting : dependentTasks.get(current.getName())) {
                depCounts.put(waiting, depCounts.get(waiting) - 1);
                if (depCounts.get(waiting) == 0) {
                    queue.offer(taskMap.get(waiting));
                }
            }
        }
        if(orderedTasks.size()!=tasks.size()) {
        	throw new IllegalStateException("Cycle detected in startup task dependencies!");
        }
		return orderedTasks;
	}
}
