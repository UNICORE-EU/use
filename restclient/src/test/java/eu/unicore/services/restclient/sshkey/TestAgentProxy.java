package eu.unicore.services.restclient.sshkey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.sshkey.SSHAgentProxy.Identity;

public class TestAgentProxy {

	static MockSSHAgent agent;

	@BeforeAll
	public static void startAgent() throws Exception {
		FileUtils.deleteQuietly(new File("./target/SSH_AGENT"));
		agent = new MockSSHAgent("./target/SSH_AGENT");
		Thread agentThread = new Thread(()->agent.start());
		agentThread.start();
		Thread.sleep(100);
	}

	@AfterAll
	public static void stopAgent() throws Exception {
		if(agent!=null)agent.stop();
		FileUtils.deleteQuietly(new File("./target/SSH_AGENT"));
	}

	@Test
	public void testIdentities() throws Exception {
		SSHAgentProxy ap = new SSHAgentProxy("./target/SSH_AGENT");
		assertTrue(ap.isAvailable());
		agent.identities.add(new Identity("this is the blob".getBytes(),"some comment".getBytes()));
		Identity[] ids = ap.getIdentities();
		assertEquals(1,  ids.length);
		Identity i = ids[0];
		assertEquals("this is the blob", new String(i.getBlob()));
		assertEquals("some comment", new String(i.getComment()));
	}

	@Test
	public void testSign() throws Exception {
		SSHAgentProxy ap = new SSHAgentProxy("./target/SSH_AGENT");
		assertTrue(ap.isAvailable());
		Identity i = new Identity("this is the blob".getBytes(),"some comment".getBytes());
		byte[] sig = ap.sign(i.getBlob(), "mock data".getBytes());
		assertEquals("mock signature", new String(sig));
	}

}
