/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 08-11-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import eu.unicore.bugsreporter.annotation.RegressionTest;
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
	public void testAdmin()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("admin", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"http://serviceName", "default_resource", 
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
	public void testBanned()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("banned", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("QueryResourceProperties", OperationType.read);
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
	@RegressionTest(url="https://sourceforge.net/tracker/index.php?func=detail&aid=3429753&group_id=102081&atid=633902")
	public void testNoAction()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("user", 
					"CN=Testing Tester,C=XX");
			ResourceDescriptor des = new ResourceDescriptor(
					"TargetSystemFactoryService", "default_resource", 
					"CN=Testing Owner,C=XX");

			PDPResult result = pdp.checkAuthorisation(c, null, des);
			
			assertTrue(result.getDecision().equals(Decision.PERMIT));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void testUser()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("user", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"StorageManagement", "default_storage", 
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
	public void testOwner()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("user", 
					"CN=Testing Owner,C=XX");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"StorageManagement", "qwerty", 
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
	public void testDestroySMS()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("user", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("Destroy", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"StorageManagement", "default_storage", 
					"CN=Testing Owner,C=XX");

			PDPResult result = pdp.checkAuthorisation(c, action, des);
			
			assertTrue(result.getMessage(), result.getDecision().equals(Decision.DENY));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}		
	}

	@Test
	public void testDeny()
	{
		try
		{
			Client c = MockAuthZContext.createRequest("anonymous", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("createTSS", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"TargetSystemFactoryService", "default", 
					"CN=Testing Owner,C=XX");

			PDPResult result = pdp.checkAuthorisation(c, action, des);
			
			assertTrue(result.getDecision().equals(Decision.DENY));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}		
	}

}
