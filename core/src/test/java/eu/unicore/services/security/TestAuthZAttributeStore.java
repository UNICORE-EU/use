package eu.unicore.services.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.security.util.AuthZAttributeStore;

public class TestAuthZAttributeStore {
	boolean ok = false;

	@Test
	public void test1()throws InterruptedException{
		final Client c = new Client();
		final SecurityTokens t = new SecurityTokens();
		AuthZAttributeStore.setTokens(t);
		t.setUserName("CN=dummy");
		t.setConsignorTrusted(true);
		c.setAuthenticatedClient(t);
		assertEquals(Client.Type.AUTHENTICATED, c.getType());
		assertEquals(t, AuthZAttributeStore.getTokens());
		AuthZAttributeStore.setClient(c);
		assertEquals(c, AuthZAttributeStore.getClient());
		assertEquals(t, AuthZAttributeStore.getTokens());
		AuthZAttributeStore.clear();
		assertNull(AuthZAttributeStore.getClient());
		assertNull(AuthZAttributeStore.getTokens());
	}

}
