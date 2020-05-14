package de.fzj.unicore.wsrflite.utils;

import de.fzj.unicore.wsrflite.Capabilities;
import de.fzj.unicore.wsrflite.Capability;

public class MockCapabilities implements Capabilities {

	@Override
	public Capability[] getCapabilities() {
		return new Capability[]{new MockCapability()};
	}
	
	
	public static class MockCapability implements Capability{
		
		public Class<?> getImplementation() {
			return String.class;
		}

		public Class<?> getInterface() {
			return String.class;
		}
		
	}
}
