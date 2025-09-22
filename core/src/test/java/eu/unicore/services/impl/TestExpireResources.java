package eu.unicore.services.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.persistence.Store;

public class TestExpireResources {
	private Home home;
	private Resource item1;
	private Resource item2;
	private boolean destroyed=false;
	private boolean destroyed2=false;
	
	@BeforeEach
	public void setUp()throws Exception{
		
		item1 = new ResourceImpl(){
	
			public String getUniqueID() {
				return "testid";
			}

			@Override
			public void destroy() {
				destroyed=true;
				super.destroy();
			}

		};

		item2 = new ResourceImpl(){

			public String getUniqueID() {
				return "testid2";
			}

			public void destroy() {
				destroyed2=true;
				super.destroy();
			}

		};
		
		home = new DefaultHome(){

			@Override
			protected Integer getMaxLifetime() {
				return null;
			}

			public String getServiceName(){return "test123";}

			@Override
			public void start(String n)throws Exception{
				serviceInstances=createStore();
				assertNotNull(serviceInstances);
				//set TT of "testid" to Now minus one hour
				Calendar c=Calendar.getInstance();
				c.add(Calendar.HOUR, -1);
				try {
					setTerminationTime("testid", c);
					setTerminationTime("testid2", c);
				}catch(Exception e){
					e.printStackTrace();
				}
			}

			@Override
			protected Resource doCreateInstance() {
				return null;
			}

			@Override
			protected void cleanupResource(String resourceId, String owner)throws Exception {
				super.cleanupResource(resourceId, owner);
			}

			@Override
			public Resource get(String resourceId) {
				if(resourceId.equals(item1.getUniqueID()))return item1;
				if(resourceId.equals(item2.getUniqueID()))return item2;
				throw new ResourceUnknownException("Unknown: "+resourceId);
			}

			@Override
			public Resource getForUpdate(String id)
			throws ResourceUnknownException,
			ResourceUnavailableException {
				if(id.equals(item1.getUniqueID()))return item1;
				if(id.equals(item2.getUniqueID()))return item2;
				throw new ResourceUnknownException("Unknown: "+id);
			}

		};
		home.start("test");
		item1.setHome(home);
		item2.setHome(home);
	}

	protected Store createStore() throws Exception{
		return new MockStore();
	}

	@Test
	public void testRemoveExpiredInstance()throws Exception{
		InstanceChecking ic = new InstanceChecking(home);
		assertTrue(ic.add(item1.getUniqueID()));
		assertTrue(ic.addChecker(new ExpiryChecker()));
		ic.run();
		assertFalse(ic.remove(item1.getUniqueID()));
		Calendar tt=home.getTerminationTime(item1.getUniqueID());
		assertNull(tt);
	}

	@Test
	public void testScheduled() throws Exception{
		InstanceChecking ic = new InstanceChecking(home);
		assertTrue(ic.add(item1.getUniqueID()));
		assertTrue(ic.addChecker(new ExpiryChecker()));
		ScheduledExecutorService reaper = Executors.newScheduledThreadPool(1);
		//check instances for expiry every 10 milliseconds
		reaper.scheduleAtFixedRate(ic,10,10,TimeUnit.MILLISECONDS);
		Thread.sleep(500);
		assertFalse(ic.list.contains(item1.getUniqueID()));
		assertTrue(destroyed);
		Calendar tt=home.getTerminationTime(item1.getUniqueID());
		assertNull(tt);
	}
	
	@Test
	public void testScheduled2() throws Exception{
		InstanceChecking ic = new InstanceChecking(home);
		assertTrue(ic.add(item1.getUniqueID()));
		assertTrue(ic.add(item2.getUniqueID()));
		assertTrue(ic.addChecker(new ExpiryChecker()));
		ic.run();
		assertFalse(ic.list.contains(item1.getUniqueID()));
		assertTrue(destroyed);
		Calendar tt=home.getTerminationTime(item1.getUniqueID());
		assertNull(tt);
		assertFalse(ic.list.contains(item2.getUniqueID()));
		assertTrue(destroyed2);
		Calendar tt2=home.getTerminationTime(item2.getUniqueID());
		assertNull(tt2);
	}

}
