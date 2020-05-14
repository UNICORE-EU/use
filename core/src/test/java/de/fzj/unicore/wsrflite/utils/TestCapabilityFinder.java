package de.fzj.unicore.wsrflite.utils;

import java.util.Iterator;
import java.util.ServiceLoader;

import de.fzj.unicore.wsrflite.Capabilities;
import de.fzj.unicore.wsrflite.Capability;

import junit.framework.TestCase;

public class TestCapabilityFinder extends TestCase {

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
