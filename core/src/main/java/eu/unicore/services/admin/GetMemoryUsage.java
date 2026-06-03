package eu.unicore.services.admin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

import eu.unicore.services.Kernel;

/**
 * get an overview of memory usage
 * @author schuller
 */
public class GetMemoryUsage implements AdminAction {

	@Override
	public String getName() {
		return "GetMemoryUsage";
	}

	@Override
	public String getDescription() {
		return "get an overview of the server process' memory usage";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		String msg = "OK";
		MemoryMXBean b1 = ManagementFactory.getMemoryMXBean();
		int mb = 1024*1024;
		AdminActionResult res = new AdminActionResult(true, msg);
		MemoryUsage nonHeap = b1.getNonHeapMemoryUsage();
		res.getResults().put("NonHeap",
				String.format("Committed:%sMB,Used:%sMB,Max:%sMB",
						nonHeap.getCommitted()/mb, nonHeap.getUsed()/mb, nonHeap.getMax()/mb));
		MemoryUsage heap = b1.getHeapMemoryUsage();
		res.getResults().put("Heap",
				String.format("Committed:%sMB,Used:%sMB,Max:%sMB",
						heap.getCommitted()/mb, heap.getUsed()/mb, heap.getMax()/mb));
		for(MemoryPoolMXBean p: ManagementFactory.getMemoryPoolMXBeans()) {
			MemoryUsage pool = p.getUsage();
			res.getResults().put("Pool_"+p.getName(),
					String.format("Committed:%sMB,Used:%sMB,Max:%sMB",
							pool.getCommitted()/mb, pool.getUsed()/mb, pool.getMax()/mb));
		}
		return res;
	}

}
