package de.fzj.unicore.wsrflite.impl;

import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;

public class MockHome extends DefaultHome {

	public String extractServiceReference(Object context)
			throws ResourceUnknownException {
		return "123";
	}

	@Override
	protected Resource doCreateInstance() {
		return null;
	}

	public boolean terminationTimesMapContainsID(String id){
		return terminationTimes.containsKey(id);
	}
	
}
