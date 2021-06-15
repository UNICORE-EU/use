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
 

package eu.unicore.services.messaging;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.persist.PersistenceProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;
import junit.framework.TestCase;


public class TestMessaging extends TestCase {
	
	private static String dir="target/data-tests";
	
	@Before
	public void setUp()throws Exception{
		FileUtils.deleteQuietly(new File(dir));
	}
	
	@After
	public void tearDown(){
		FileUtils.deleteQuietly(new File(dir));
	}
	
	protected MessagingImpl getMessaging()throws Exception{
		Properties p=TestConfigUtil.getInsecureProperties();
		p.setProperty(PersistenceProperties.PREFIX+PersistenceProperties.DB_DIRECTORY, dir);
		Kernel k=new Kernel(p);
		MessagingImpl sm=(MessagingImpl)k.getMessaging();
		return sm;
	}
	
	@Test
	public void test1() throws Exception{
		MessagingImpl sm=getMessaging();
		sm.cleanup();
		IMessagingChannel p=sm.getChannel("test123");
		
		Message m1=new Message("message1");
		Message m2=new Message("message2");
		
		p.publish(m1);
		p.publish(m2);
		
		assertEquals(2, sm.getStoredMessages());
		
		PullPoint pull=sm.getPullPoint("test123");
		assertNotNull(pull);
		while(pull.hasNext()){
			Message m=pull.next();
			String body=String.valueOf(m.getBody());
			assertTrue(body.contains("message"));
		}
		assertEquals(sm.getStoredMessages(),0);
		
		m1=new Message("message1");
		m2=new Message("message2");
		
		p.publish(m1);
		p.publish(m2);
		
		IMessagingChannel p2=sm.getChannel("test123");
		
		m1=new Message("message3");
		m2=new Message("message4");
		
		p2.publish(m1);
		p2.publish(m2);
		
		assertEquals(sm.getStoredMessages(),4);
		
		
	}
	
	@Test
	public void testCreateDestination() throws Exception{
		try{
			MessagingImpl sm=getMessaging();
			sm.getChannel("test123");
			sm.getChannel("test123");
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}	
	}
	
	@Test
	public void test2() throws Exception {
		MessagingImpl sm=getMessaging();
		sm.cleanup();
		IMessagingChannel p=sm.getChannel("test123");
		
		p.publish(new Message("message1"));
		p.publish(new Message("message2"));
		p.publish(new Message("message3"));
		
		assertTrue(sm.hasMessages("test123"));
		PullPoint pp=sm.getPullPoint("test123");
		assertFalse(sm.hasMessages("test123"));
		pp.dispose();
		assertTrue(sm.hasMessages("test123"));
		assertEquals(3, sm.getStoredMessages());
		pp=sm.getPullPoint("test123");
		pp.next();
		pp.dispose();
		assertTrue(sm.hasMessages("test123"));
		assertEquals(2, sm.getStoredMessages());
		
	}
	
}
