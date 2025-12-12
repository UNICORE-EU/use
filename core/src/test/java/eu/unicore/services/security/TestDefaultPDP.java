package eu.unicore.services.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.Role;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.DefaultPDP;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;;

public class TestDefaultPDP {

	@Test
	public void testAdmin() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		Client c = new Client();
		c.setRole(new Role("admin",""));
		assertEquals(Decision.PERMIT, pdp.checkAuthorisation(c, null, null).getDecision());
	}

	@Test
	public void testBan() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		Client a = new Client();
		a.setRole(new Role("banned",""));
		assertEquals(Decision.DENY, pdp.checkAuthorisation(a, null, null).getDecision());
	}

	@Test
	public void testDelete() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		ActionDescriptor a = new ActionDescriptor("DELETE", OperationType.modify);
		assertEquals(Decision.DENY, pdp.checkAuthorisation(null, a, null).getDecision());
	}

	@Test
	public void testACL() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		ResourceDescriptor d = new ResourceDescriptor("foo","bar", "CN=someone");
		d.setAclCheckOK(true);
		assertEquals(Decision.PERMIT, pdp.checkAuthorisation(null, null, d).getDecision());
	}

	@Test
	public void testOwner() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		Client c = new Client();
		SecurityTokens t = new SecurityTokens();
		t.setUserName("CN=someone");
		t.setConsignorTrusted(true);
		c.setAuthenticatedClient(t);
		ResourceDescriptor d = new ResourceDescriptor("foo","bar", "CN=someone");
		assertEquals(Decision.PERMIT, pdp.checkAuthorisation(c, null, d).getDecision());
	}

	@Test
	public void testPerService() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		pdp.setServiceRules("foo", Arrays.asList(DefaultPDP.PERMIT_READ));
		ActionDescriptor a = new ActionDescriptor("GET", OperationType.read);
		ResourceDescriptor d = new ResourceDescriptor("foo","bar", "CN=someone");
		assertEquals(Decision.PERMIT, pdp.checkAuthorisation(null, a, d).getDecision());
	}

	@Test
	public void testPerServicePermitUser() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		pdp.setServiceRules("foo", Arrays.asList(DefaultPDP.PERMIT_USER));
		Client c = new Client();
		c.setRole(new Role("user",""));
		ResourceDescriptor d = new ResourceDescriptor("foo","bar", "CN=someone");
		assertEquals(Decision.PERMIT, pdp.checkAuthorisation(c,null,d).getDecision());
	}

	@Test
	public void testFinalDeny() throws Exception {
		DefaultPDP pdp = new DefaultPDP();
		Client c = new Client();
		ActionDescriptor a = new ActionDescriptor("GET", OperationType.read);
		ResourceDescriptor d = new ResourceDescriptor("foo","bar", "CN=someone");
		assertEquals(Decision.DENY, pdp.checkAuthorisation(c, a, d).getDecision());
	}

}