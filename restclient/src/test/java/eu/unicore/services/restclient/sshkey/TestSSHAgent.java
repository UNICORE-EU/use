package eu.unicore.services.restclient.sshkey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Base64;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.sshkey.SSHAgentProxy.Identity;

public class TestSSHAgent {

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
		agent.identities.clear();
		agent.identities.add(new Identity("this is the blob".getBytes(),"some comment".getBytes()));
		Identity[] ids = ap.getIdentities();
		assertEquals(1,  ids.length);
		Identity i = ids[0];
		assertEquals("this is the blob", new String(i.getBlob()));
		assertEquals("some comment", new String(i.getComment()));
	}

	@Test
	public void testSSHAgent() throws Exception {
		SSHAgent a = setupAgent(false);
		assertEquals("ssh-ed25519", a.getAlgorithm());
		assertNotNull(a.getSigner());
	}
	
	@Test
	public void testSSHAgentKey() throws Exception {
		SSHAgent a = setupAgent(true);
		SSHAgentKeyAuthN auth = new SSHAgentKeyAuthN("nobody", a);
		HttpGet get = new HttpGet("https://foo.com");
		auth.addAuthenticationHeaders(get);
		assertNotNull(get.getHeader("Authorization"));
		assertTrue(auth.tokenStillValid());
	}
	
	private SSHAgent setupAgent(boolean select) throws Exception {
		String keyFile = "src/test/resources/ssh/id_ed25519";
		agent.keyFile = keyFile;
		agent.pass = "test123";
		String pubkey = FileUtils.readFileToString(new File(keyFile+".pub"), "UTF-8");
		StringTokenizer st = new StringTokenizer(pubkey);
		st.nextToken(); // ignored
		String base64 = st.nextToken();
		byte[] blob = Base64.getDecoder().decode(base64);
		byte[] comment = st.nextToken().getBytes();
		Identity id = new Identity(blob, comment);
		agent.identities.clear();
		agent.identities.add(id);
		SSHAgentProxy ap = new SSHAgentProxy("./target/SSH_AGENT");
		assertTrue(ap.isAvailable());
		SSHAgent a = new SSHAgent(ap);
		a.setLogger(null);
		if(select)a.selectIdentity(keyFile);
		else a.assertIdentity();
		assertNotNull(a.id);
		assertTrue(Arrays.areEqual(a.id.getBlob(), id.getBlob()));
		return a;
	}
	
}
