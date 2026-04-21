package eu.unicore.services.pdp.local;

import static eu.unicore.services.security.ContainerSecurityProperties.PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_CHECKACCESS;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_CHECKACCESS_PDP;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_CHECKACCESS_PDPCONFIG;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_GATEWAY_ENABLE;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_SSL_ENABLED;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.Kernel;
import eu.unicore.services.USEClientProperties;
import eu.unicore.services.pdp.MockAuthZContext;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.httpclient.ClientProperties;


public class LocalPDPTest
{
	protected UnicoreXPDP pdp;

	@BeforeEach
	public void setUp() throws Exception
	{
		String f = "src/test/resources/local/pdp2.conf";
		pdp = new LocalHerasafPDP();
		((LocalHerasafPDP)pdp).initialize(f, "http://test123.local");
		((LocalHerasafPDP)pdp).lps.reload(f);
	}

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

	@Test
	public void testTriggerReload() throws Exception
	{
		Properties props = new Properties();
		props.setProperty(PREFIX+PROP_SSL_ENABLED, "false");
		props.setProperty(PREFIX+PROP_GATEWAY_ENABLE, "false");
		props.setProperty(USEClientProperties.PREFIX+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		props.setProperty(PREFIX+PROP_CHECKACCESS, "true");
		props.setProperty(PREFIX+PROP_CHECKACCESS_PDP, LocalHerasafPDP.class.getName());
		props.setProperty(PREFIX+PROP_CHECKACCESS_PDPCONFIG, "src/test/resources/local/pdp2.conf");
		String dir = "target/kerneldata";
		FileUtils.deleteQuietly(new File(dir));
		props.setProperty("persistence.directory", dir);
		Kernel k = new Kernel(props);
		pdp.setKernel(k);
		System.out.println(((LocalHerasafPDP)pdp).getName()+": "+
				((LocalHerasafPDP)pdp).getStatusDescription());
	}

}
