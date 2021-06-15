package eu.unicore.services.impl;

import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnknownException;

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
