/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package de.fzj.unicore.wsrflite.impl;

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

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnavailableException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.persistence.Store;

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

			public void activateHome(String n)throws Exception{
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

			public void passivateHome(){};

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
				return null;
			}

			@Override
			public Resource getForUpdate(String id)
			throws ResourceUnknownException,
			ResourceUnavailableException {
				if(id.equals(item.getUniqueID()))return item;
				if(id.equals(item2.getUniqueID()))return item2;
				return null;
			}

		};
		home.activateHome("test");
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
