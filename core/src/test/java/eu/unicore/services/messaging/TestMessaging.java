package eu.unicore.services.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.PersistenceProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;


public class TestMessaging {

	private static String dir="target/data-tests";

	@BeforeEach
	public void setUp()throws Exception{
		FileUtils.deleteQuietly(new File(dir));
	}

	@AfterEach
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
		p.flush();
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
		p.flush();

		IMessagingChannel p2=sm.getChannel("test123");

		m1=new Message("message3");
		m2=new Message("message4");

		p2.publish(m1);
		p2.publish(m2);
		p2.flush();

		assertEquals(sm.getStoredMessages(),4);
	}

	@Test
	public void testCreateDestination() throws Exception{
		MessagingImpl sm=getMessaging();
		sm.getChannel("test123");
		sm.getChannel("test123");
	}

	@Test
	public void test2() throws Exception {
		MessagingImpl sm=getMessaging();
		sm.cleanup();
		IMessagingChannel p=sm.getChannel("test123");

		p.publish(new Message("message1"));
		p.publish(new Message("message2"));
		p.publish(new Message("message3"));
		p.flush();
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
