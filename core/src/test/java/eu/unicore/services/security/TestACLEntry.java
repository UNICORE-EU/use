package eu.unicore.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.Role;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.Xlogin;

public class TestACLEntry {

	@Test
	public void testMatchDN() throws Exception {
		ACLEntry e = ACLEntry.parse("read:DN:CN=foo");
		System.out.println(e);
		Client c1 = new Client();
		SecurityTokens st = new SecurityTokens();
		st.setUserName("CN=foo");
		st.setConsignorTrusted(true);
		c1.setAuthenticatedClient(st);
		c1.setRole(new Role("user", "testing role"));
		assertTrue(e.allowed(OperationType.read, c1));
		assertFalse(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
		st.setUserName("CN=other");
		assertFalse(e.allowed(OperationType.read, c1));
		assertFalse(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
	}
	
	@Test
	public void testMatchRole() throws Exception {
		ACLEntry e = ACLEntry.parse("write:ROLE:user");
		Client c1 = new Client();
		SecurityTokens st = new SecurityTokens();
		st.setUserName("CN=foo");
		st.setConsignorTrusted(true);
		c1.setAuthenticatedClient(st);
		c1.setRole(new Role("user", "testing role"));
		assertTrue(e.allowed(OperationType.read, c1));
		assertTrue(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
		c1.setRole(new Role("some", "testing role"));
		assertFalse(e.allowed(OperationType.read, c1));
		assertFalse(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
	}

	@Test
	public void testMatchUID() throws Exception {
		ACLEntry e = ACLEntry.parse("write:UID:hpc1");
		Client c1 = new Client();
		SecurityTokens st = new SecurityTokens();
		st.setUserName("CN=foo");
		st.setConsignorTrusted(true);
		c1.setAuthenticatedClient(st);
		c1.setXlogin(new Xlogin(new String[]{"hpc1"}));
		assertTrue(e.allowed(OperationType.read, c1));
		assertTrue(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
		c1.setXlogin(new Xlogin(new String[]{"hpc2"}));
		assertFalse(e.allowed(OperationType.read, c1));
		assertFalse(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
	}
	
	@Test
	public void testMatchGroup() throws Exception {
		ACLEntry e = ACLEntry.parse("write:GROUP:hpc1");
		Client c1 = new Client();
		SecurityTokens st = new SecurityTokens();
		st.setUserName("CN=foo");
		st.setConsignorTrusted(true);
		c1.setAuthenticatedClient(st);
		Xlogin x = new Xlogin(new String[]{"user1"}, new String[]{"hpc1"});
		c1.setXlogin(x);
		assertTrue(e.allowed(OperationType.read, c1));
		assertTrue(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
		x = new Xlogin(new String[]{"user2"}, new String[]{"othergrp"});
		c1.setXlogin(x);
		assertFalse(e.allowed(OperationType.read, c1));
		assertFalse(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
	}
	@Test
	public void testMatchVO() throws Exception {
		ACLEntry e = ACLEntry.parse("write:VO:foo");
		Client c1 = new Client();
		SecurityTokens st = new SecurityTokens();
		st.setUserName("CN=foo");
		st.setConsignorTrusted(true);
		c1.setAuthenticatedClient(st);
		c1.setVos(new String[]{"foo", "spam"});
		c1.setVo("foo");
		assertTrue(e.allowed(OperationType.read, c1));
		assertTrue(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
		c1.setVo("spam");
		assertFalse(e.allowed(OperationType.read, c1));
		assertFalse(e.allowed(OperationType.write, c1));
		assertFalse(e.allowed(OperationType.modify, c1));
	}

	@Test
	public void testParse() {
		assertThrows(IllegalArgumentException.class, ()->{
			ACLEntry.parse("nosuchop:DN:cn=foo");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			ACLEntry.parse("read:nosuchtype:cn=foo");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			ACLEntry.parse("read:DN:baddn");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			new ACLEntry(null, null, null);
		});
	}
}
