package eu.unicore.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExtendedResourceStatus.ResourceStatus;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.security.util.AuthZAttributeStore;

public class TestResourceImpl {

	private ResourceImpl makeResource(InitParameters initMap)throws Exception{
		return makeResource(null, initMap);
	}
	
	private ResourceImpl makeResource(Properties p, InitParameters initMap)throws Exception{
		if (p == null) {
			p = TestConfigUtil.getInsecureProperties();
		}
		if (initMap == null){
			initMap = new InitParameters("123");
		}
		Kernel kernel = new Kernel(p);
		ResourceImpl r=new _ResourceImpl();
		MockHome h=new MockHome();
		h.setKernel(kernel);
		h.start("test");
		r.setHome(h);
		r.setKernel(kernel);
		r.initialise(initMap);
		return r;
	}
	
	@Test
	public void testSetTerminationTime() throws Exception {
		Calendar c=new GregorianCalendar();
		c.add(Calendar.YEAR,1);
		ResourceImpl r=makeResource(null);
		r.getHome().setTerminationTime("123", c);
		//get tt
		Calendar tt=r.getHome().getTerminationTime("123");
		assertNotNull(tt);
		assertEquals(tt.getTimeInMillis(),c.getTimeInMillis());
	}

	@Test
	public void testSetUniqueIdAndServiceName() throws Exception {
		Resource r=makeResource(null);
		assertEquals("123",r.getUniqueID());
		assertEquals("test",r.getServiceName());
	}

	@Test
	public void testSetInitialTerminationTime() throws Exception {
		Calendar c=new GregorianCalendar();
		c.add(Calendar.YEAR,1);
		InitParameters init = new InitParameters(null,c);
		Resource r=makeResource(init);
		String id=r.getUniqueID();
		//get tt
		Calendar tt=r.getHome().getTerminationTime(id);
		assertNotNull(tt);
		assertEquals(tt.getTimeInMillis(),c.getTimeInMillis());
		assertEquals(r.getHome().getTerminationTime(r.getUniqueID()).getTimeInMillis(),tt.getTimeInMillis());
		
	}
	
	@Test
	public void testExceedSystemMaxTermTime(){
		Calendar c=Calendar.getInstance();
		c.add(Calendar.SECOND, 1500);
		InitParameters initobjs = new InitParameters(null, c);
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.MAXIMUM_LIFETIME, "1000");
		try{
			makeResource(p, initobjs);
			fail("Expected exception due to exceeded termtime.");
		}
		catch(Exception ex){
			//OK
		}
	}

	@Test
	public void testNotExceedSystemMaxTermTime() throws Exception {
		Calendar c=Calendar.getInstance();
		c.add(Calendar.SECOND, 1500);
		Properties p = TestConfigUtil.getInsecureProperties();
		InitParameters initobjs = new InitParameters(null, c);
		ResourceImpl ws=makeResource(p, initobjs);
		assertNotNull(ws);
	}

	@Test
	public void testLimitServiceInstancesPerUser()throws Exception{
		MockHome h=new MockHome(){
			@Override
			protected Resource doCreateInstance(){
				return new _ResourceImpl();
			}
		};
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.MAX_INSTANCES+".test", "2");
		Kernel kernel = new Kernel(p);

		h.setKernel(kernel);
		h.start("test");

		InitParameters initobjs=new InitParameters();

		SecurityTokens secTokens=new SecurityTokens();
		secTokens.setUserName("cn=test");
		secTokens.setConsignorTrusted(true);
		Client c = new Client();
		c.setAuthenticatedClient(secTokens);
		AuthZAttributeStore.setClient(c);

		String id=h.createResource(initobjs);
		assertNotNull(h.getStore().read(id));
		assertNotNull(id);
		id=h.createResource(initobjs);
		assertNotNull(id);
		assertThrows(ResourceNotCreatedException.class, ()->{
			h.createResource(initobjs);
		});
	}

	@Test
	public void testResourceStatus()throws Exception{
		final ResourceImpl x=makeResource(null);
		assertEquals(ResourceStatus.READY,x.getResourceStatus());
		assertEquals("N/A",x.getResourceStatusMessage());
		x.setResourceStatus(ResourceStatus.DISABLED);
		assertEquals(ResourceStatus.DISABLED,x.getResourceStatus());
		x.setResourceStatusMessage("OK");
		assertEquals("OK",x.getResourceStatusMessage());
	}

	@Test
	public void testRemoveChildren() throws Exception {
		ResourceImpl r=makeResource(null);
		r.getModel().addChild("foo", "123");
		r.getModel().addChild("bar", "456");
		r.getModel().addChild("bar", "789");
		assertFalse(r.getModel().getChildren().get("foo").isEmpty());
		assertFalse(r.getModel().getChildren().get("bar").isEmpty());
		Collection<String>res = r.deleteChildren(Arrays.asList(new String[]{"123","456","789"}));
		assertNotNull(res);
		assertEquals(3,res.size());
		assertTrue(r.getModel().getChildren().get("foo").isEmpty());
		assertTrue(r.getModel().getChildren().get("bar").isEmpty());
	}
	
	public static class _ResourceImpl extends ResourceImpl {
		
	}
}
