package de.fzj.unicore.wsrflite.security;

import junit.framework.TestCase;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;

public class TestAuthZAttributeStore extends TestCase{
	boolean ok = false;

	public void test1()throws InterruptedException{
		final Client c=new Client();
		final SecurityTokens t=new SecurityTokens();
		t.setUserName("CN=dummy");
		t.setConsignorTrusted(true);
		c.setAuthenticatedClient(t);
		assertEquals(Client.Type.AUTHENTICATED, c.getType());

		AuthZAttributeStore.setClient(c);
		assertEquals(c, AuthZAttributeStore.getClient());
		assertEquals(t, AuthZAttributeStore.getTokens());

		Thread thr=new Thread(new Runnable(){
			public void run(){
				assertEquals(Client.Type.ANONYMOUS, 
					AuthZAttributeStore.getClient().getType());
				ok = true;
			}
		});
		thr.start();
		thr.join();
		assertTrue(ok);
		AuthZAttributeStore.clear();
		assertEquals(Client.Type.ANONYMOUS, AuthZAttributeStore.getClient().getType());
		assertEquals("anonymous", AuthZAttributeStore.getClient().getRole().getName());
		assertNull(AuthZAttributeStore.getTokens());
	}

}
