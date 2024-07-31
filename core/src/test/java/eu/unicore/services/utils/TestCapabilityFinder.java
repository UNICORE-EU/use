package eu.unicore.services.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;

public class TestCapabilityFinder {

	@Test
	public void testLoadCapabilities(){
		ServiceLoader<Capabilities> sl=ServiceLoader.load(Capabilities.class);
		Iterator<Capabilities>iter=sl.iterator();
		int i=0;
		while(iter.hasNext()){
			Capability c=iter.next().getCapabilities()[0];
			assertEquals(String.class,c.getImplementation());
			assertEquals(String.class,c.getInterface());
			i++;
		}
		assertEquals(1, i);
	}
	
	
	
}
