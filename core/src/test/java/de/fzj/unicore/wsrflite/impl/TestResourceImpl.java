/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

package de.fzj.unicore.wsrflite.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Properties;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.ExtendedResourceStatus.ResourceStatus;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.ErrorCodes;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import junit.framework.TestCase;


/**
 * some tests for the ResourceImpl
 * 
 * @author schuller
 */
public class TestResourceImpl extends TestCase {

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
		h.activateHome("test");
		r.setHome(h);
		r.setKernel(kernel);
		r.initialise(initMap);
		return r;
	}
	
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

	public void testSetUniqueIdAndServiceName() throws Exception {
		Resource r=makeResource(null);
		assertEquals("123",r.getUniqueID());
		assertEquals("test",r.getServiceName());
	}
	
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

	public void testNotExceedSystemMaxTermTime(){
		Calendar c=Calendar.getInstance();
		c.add(Calendar.SECOND, 1500);
		Properties p = TestConfigUtil.getInsecureProperties();
		InitParameters initobjs = new InitParameters(null, c);
		try{
			ResourceImpl ws=makeResource(p, initobjs);
			assertNotNull(ws);
		}
		catch(Exception ex){
			fail("Unexpected exception: "+ex);
		}
	}

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
		h.activateHome("test");
		
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
		try{
			id=h.createResource(initobjs);
			fail("Service limit should be exceeded");
		}catch(ResourceNotCreatedException rnc){
			//expected... check it is due to the correct reason
			assertEquals(ErrorCodes.ERR_INSTANCE_LIMIT_EXCEEDED, rnc.getErrorCode());
		}
	}
	
	public void testResourceStatus()throws Exception{
		final ResourceImpl x=makeResource(null);
		assertEquals(ResourceStatus.READY,x.getResourceStatus());
		assertEquals("N/A",x.getStatusMessage());
		x.setResourceStatus(ResourceStatus.DISABLED);
		assertEquals(ResourceStatus.DISABLED,x.getResourceStatus());
		x.setStatusMessage("OK");
		assertEquals("OK",x.getStatusMessage());
	}

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
