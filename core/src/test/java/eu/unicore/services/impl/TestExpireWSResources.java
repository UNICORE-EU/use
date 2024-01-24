package eu.unicore.services.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.persistence.Store;

public class TestExpireWSResources {
	private Home home;
	private Resource item;
	private Resource item2;
	private boolean destroyed=false;
	private boolean destroyed2=false;
	
	@Before
	public void setUp()throws Exception{
		
		item=new ResourceImpl(){
			
			public String getUniqueID() {
				return "testid";
			}
		
			@Override
			public void destroy() {
				destroyed=true;
			}

		};

		item2=new ResourceImpl(){

			
			public String getUniqueID() {
				return "testid2";
			}
	
			public void destroy() {
				destroyed2=true;
			}

		};
		
		home=new DefaultHome(){

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

			public void destroyResource(String resourceId)throws Exception {
				if(!resourceId.equals("testid")) throw new RuntimeException();
				super.destroyResource(resourceId);
			}
			@Override
			public Resource get(String resourceId) {
				if(resourceId.equals(item.getUniqueID()))return item;
				if(resourceId.equals(item2.getUniqueID()))return item2;
				throw new ResourceUnknownException("Unknown: "+resourceId);
			}

			@Override
			public Resource getForUpdate(String id)
			throws ResourceUnknownException,
			ResourceUnavailableException {
				if(id.equals(item.getUniqueID()))return item;
				if(id.equals(item2.getUniqueID()))return item2;
				throw new ResourceUnknownException("Unknown: "+id);
			}

		};
		home.start("test");
	}

	protected Store createStore() throws Exception{
		return new MockStore();
	}

	@Test
	public void testRemoveExpiredInstance()throws Exception{
		InstanceChecking ic = new InstanceChecking(home);
		assertTrue(ic.add(item.getUniqueID()));
		assertTrue(ic.addChecker(new ExpiryChecker()));
		ic.run();
		assertFalse(ic.remove(item.getUniqueID()));
		Calendar tt=home.getTerminationTime(item.getUniqueID());
		assertNull(tt);
	}

	@Test
	public void testScheduled() throws Exception{
		InstanceChecking ic = new InstanceChecking(home);
		assertTrue(ic.add(item.getUniqueID()));
		assertTrue(ic.addChecker(new ExpiryChecker()));
		ScheduledExecutorService reaper = Executors.newScheduledThreadPool(1);
		//check instances for expiry every 10 milliseconds
		reaper.scheduleAtFixedRate(ic,10,10,TimeUnit.MILLISECONDS);
		Thread.sleep(500);
		assertFalse(ic.list.contains(item.getUniqueID()));
		assertTrue(destroyed);
		Calendar tt=home.getTerminationTime(item.getUniqueID());
		assertNull(tt);
	}
	
	@Test
	public void testScheduled2() throws Exception{
		InstanceChecking ic = new InstanceChecking(home);
		assertTrue(ic.add(item.getUniqueID()));
		assertTrue(ic.add(item2.getUniqueID()));
		assertTrue(ic.addChecker(new ExpiryChecker()));
		ic.run();
		assertFalse(ic.list.contains(item.getUniqueID()));
		assertTrue(destroyed);
		Calendar tt=home.getTerminationTime(item.getUniqueID());
		assertNull(tt);
		assertFalse(ic.list.contains(item2.getUniqueID()));
		assertTrue(destroyed2);
		Calendar tt2=home.getTerminationTime(item2.getUniqueID());
		assertNull(tt2);
	}

}
