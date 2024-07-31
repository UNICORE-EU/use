package eu.unicore.services.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.security.util.AuthZAttributeStore;

public class TestAuthZAttributeStore {
	boolean ok = false;

	@Test
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
