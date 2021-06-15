package eu.unicore.services.utils;

import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;

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
