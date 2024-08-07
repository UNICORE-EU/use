package eu.unicore.services.pdp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;


public abstract class AbstractPDPTest
{
	protected UnicoreXPDP pdp;

	@Test
	public void testAdmin() throws Exception
	{
		Client c = MockAuthZContext.createRequest("admin", 
				"CN=Testing Tester,C=XX");
		ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
		ResourceDescriptor des = new ResourceDescriptor(
				"http://serviceName", "default_resource", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, action, des);
		assertTrue(result.getDecision().equals(Decision.PERMIT));
	}

	@Test
	public void testBanned() throws Exception
	{
		Client c = MockAuthZContext.createRequest("banned", 
				"CN=Testing Tester,C=XX");
		ActionDescriptor action = new ActionDescriptor("QueryResourceProperties", OperationType.read);
		ResourceDescriptor des = new ResourceDescriptor(
				"TargetSystemFactoryService", "qwerty", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, action, des);
		assertTrue(result.getDecision().equals(Decision.DENY));
	}

	@Test
	public void testNoAction() throws Exception
	{
		Client c = MockAuthZContext.createRequest("user", 
				"CN=Testing Tester,C=XX");
		ResourceDescriptor des = new ResourceDescriptor(
				"TargetSystemFactoryService", "default_resource", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, null, des);
		assertTrue(result.getDecision().equals(Decision.PERMIT));
	}

	@Test
	public void testUser() throws Exception
	{
		Client c = MockAuthZContext.createRequest("user", 
				"CN=Testing Tester,C=XX");
		ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
		ResourceDescriptor des = new ResourceDescriptor(
				"StorageManagement", "default_storage", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, action, des);
		assertTrue(result.getDecision().equals(Decision.PERMIT));
	}

	@Test
	public void testOwner() throws Exception
	{
		Client c = MockAuthZContext.createRequest("user", 
				"CN=Testing Owner,C=XX");
		ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
		ResourceDescriptor des = new ResourceDescriptor(
				"StorageManagement", "qwerty", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, action, des);
		assertTrue(result.getDecision().equals(Decision.PERMIT));
	}

	@Test
	public void testReadRp()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("anonymous", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("QueryResourceProperties", OperationType.read);
			ResourceDescriptor des = new ResourceDescriptor(
					"TargetSystemFactoryService", "qwerty", 
					"CN=Testing Owner,C=XX");

			PDPResult result = pdp.checkAuthorisation(c, action, des);

			assertTrue(result.getDecision().equals(Decision.PERMIT));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testWriteRp()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("user", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("UpdateResourceProperties", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"TargetSystemFactoryService", "qwerty", 
					"CN=Testing Owner,C=XX");

			PDPResult result = pdp.checkAuthorisation(c, action, des);

			assertTrue(result.getDecision().equals(Decision.DENY));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testDestroySMS() throws Exception
	{
		Client c = MockAuthZContext.createRequest("user", 
				"CN=Testing Tester,C=XX");
		ActionDescriptor action = new ActionDescriptor("Destroy", OperationType.modify);
		ResourceDescriptor des = new ResourceDescriptor(
				"StorageManagement", "default_storage", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, action, des);
		assertTrue(result.getDecision().equals(Decision.DENY));
	}

	@Test
	public void testDeny() throws Exception
	{
		Client c = MockAuthZContext.createRequest("anonymous", 
				"CN=Testing Tester,C=XX");
		ActionDescriptor action = new ActionDescriptor("createTSS", OperationType.modify);
		ResourceDescriptor des = new ResourceDescriptor(
				"TargetSystemFactoryService", "default", 
				"CN=Testing Owner,C=XX");
		PDPResult result = pdp.checkAuthorisation(c, action, des);
		assertTrue(result.getDecision().equals(Decision.DENY));
	}

}
