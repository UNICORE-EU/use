/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 23-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentDocument1;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.security.wsutil.client.ConditionalGetUtil;
import eu.unicore.security.wsutil.client.ContextDSigDecider;
import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.services.ws.ResourceLifetime;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.services.ws.client.BaseWSRFClient;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.testutils.AbstractTestBase;
import eu.unicore.services.ws.testutils.JettyServer;
import eu.unicore.services.ws.testutils.MockSecurityConfig;
import eu.unicore.services.ws.testutils.MockServiceImpl;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.SessionIDProvider;

public class TestBaseWSRFClient extends AbstractTestBase
{
	@Test
	public void testTimeout() throws Exception
	{
		MockServiceImpl.slowResponse=true;
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl");
		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setMessageLogging(true);
		sec.getHttpClientProperties().setConnectionTimeout(500);
		sec.getHttpClientProperties().setSocketTimeout(500);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		
		long start = System.currentTimeMillis();
		try
		{
			client.getResourcePropertyDocument();
			fail("Timeout after: " + (System.currentTimeMillis()-start));
		} catch(Exception e)
		{
			long end = System.currentTimeMillis();
			System.out.println("Timeout after: "+ (end-start));
			assertTrue("Timeout after: " + (end-start), end-start < 1500+2000);
			MockServiceImpl.slowResponse=false;
			return;
		}
	}
	
	
	@Test
	public void testPlainHTTP() throws Exception
	{
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"http://localhost:" + (JettyServer.PORT+1) + "/services/MockServiceImpl");
		MockSecurityConfig sec = new MockSecurityConfig(false, false);
		sec.setMessageLogging(true);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		client.getResourceProperty(new QName("dummy"));
	}

	@Test
	public void testOutgoingMessageSignatureSetup()throws Exception{
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"http://localhost:" + (JettyServer.PORT+1) + "/services/MockServiceImpl");
		MockSecurityConfig sec = new MockSecurityConfig(false, false);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		Client xClient=WSClientFactory.getWSClient(client.getLT());
		@SuppressWarnings("unchecked")
		Set<String> sign=(Set<String>)xClient.getRequestContext().get(ContextDSigDecider.SIGNED_OPERATIONS);
		assertNotNull(sign);
		assertTrue(sign.contains(ResourceLifetime.WSRL_DESTROY));
	}
	
	@Test
	public void testOutgoingMessageETDReceiverSetup()throws Exception{
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"http://localhost:" + (JettyServer.PORT+1) + "/services/MockServiceImpl");
		WSUtilities.addServerIdentity(epr, "CN=Test server");
		IClientConfiguration sec =new MockSecurityConfig(true,false);
		
		sec.getETDSettings().setExtendTrustDelegation(true);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		IClientConfiguration secProps=client.getSecurityConfiguration();
		assertNotNull(secProps.getETDSettings().getReceiver());
		assertEquals("CN=Test server",secProps.getETDSettings().getReceiver().getName());
	}
	
	/**
	 * tests that ws-addressing is correctly set up, i.e. reference parameters
	 * and wsa:To are initialized correctly on outgoing calls
	 * @throws Exception
	 */
	@Test
	public void testOutgoingMessageWSASetup()throws Exception{
		String url="http://localhost:" + (JettyServer.PORT+1) + "/services/MockServiceImpl";
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("test123");
		
		WSUtilities.addReferenceParameter(epr, new QName("foo","bar"), "abcd");
		WSUtilities.addServerIdentity(epr, "CN=Test server");
		IClientConfiguration sec =new MockSecurityConfig(true,false);
		sec.getETDSettings().setExtendTrustDelegation(true);
		BaseWSRFClient client = new BaseWSRFClient(url, epr, sec);
		QName q1=new QName("foo","bar");
		String res=client.getResourceProperty(q1);
		System.out.println(res);
		assertTrue(res.toString().contains("test123"));
		assertTrue(res.toString().contains("abcd"));
	}
	
	@Test
	public void testEPRMetadata() throws Exception
	{
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl");
		WSUtilities.addServerIdentity(epr, "CN=test");
		String friendlyName="my test epr";
		WSUtilities.addFriendlyName(epr, friendlyName);
		
		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setMessageLogging(true);
		sec.getHttpClientProperties().setConnectionTimeout(500);
		sec.getHttpClientProperties().setSocketTimeout(500);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		
		assertEquals(friendlyName,client.getFriendlyName());
	}
	
	@Test
	public void testConditionalGet() throws Exception
	{
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl");
		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setMessageLogging(true);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		client.setUpdateInterval(-1);
		
		// setup server data
		MockServiceImpl.lastMod=Calendar.getInstance();
		MockServiceImpl.lastMod.add(Calendar.DATE, -1);
		MockServiceImpl.rpDocContent="test123";
		
		String rp=client.getResourcePropertyDocument();
		System.out.println(rp);
		
		// ask again
		String rp2=client.getResourcePropertyDocument();
		assertEquals(rp, rp2);
		assertTrue(ConditionalGetUtil.Client.isNotModified());
		
		// change data
		MockServiceImpl.lastMod=Calendar.getInstance();
		MockServiceImpl.rpDocContent="foobar";
		
		// ask again
		String rp3=client.getResourcePropertyDocument();
		assertNotSame(rp2, rp3);
		assertFalse(ConditionalGetUtil.Client.isNotModified());
		
	}
	
	@Test
	public void testRetry() throws Exception {
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl");
		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setMessageLogging(false);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		client.setUpdateInterval(-1);
		PutResourcePropertyDocumentDocument1 in=PutResourcePropertyDocumentDocument1.Factory.newInstance();
		in.addNewPutResourcePropertyDocument();
		MockServiceImpl.putRPException=ResourceUnavailableFault.createFault("test");
		try{
			XmlObject reply=client.getRP().PutResourcePropertyDocument(in);
			System.out.println(reply);
			
		}catch(Exception ex){
			System.out.println("Got: "+ex.getClass().getName()+
		    " message: "+Log.createFaultMessage("", ex));
		}
		System.out.println("Total calls: "+MockServiceImpl.putCalls.get());
		assertTrue(1<MockServiceImpl.putCalls.get());
	}
	
	@Test
	public void testHandleInvalidSecuritySessiom() throws Exception {
		String url = "https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl";
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(url);
		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setUseSecuritySessions(true);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		client.setUpdateInterval(-1);
		String rp=client.getResourcePropertyDocument();
		assertNotNull(rp);
		SessionIDProvider sessionProvider = sec.getSessionIDProvider();
		System.out.println(sessionProvider.getAllSessions());
		assertFalse(sessionProvider.getAllSessions().isEmpty());
		// set a wrong security session id to trigger a fault
		sessionProvider.clearAll();
		sessionProvider.registerSession("test123", url, 3600000, sec);
		client = new BaseWSRFClient(epr, sec);
		rp=client.getResourcePropertyDocument();
	}
	
	@Test
	public void testMemoryLeakMAPCodec() throws Exception {
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(
				"https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl");
		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setMessageLogging(false);
		BaseWSRFClient client = new BaseWSRFClient(epr, sec);
		MAPCodec codec=getOutgoingRPCodec(client);
		assertNotNull(codec);
		int pre=codec.getUncorrelatedExchanges().size();
		for(int i=0; i<500; i++){
			client.getResourceProperty(new QName("dummy"));
		}
		int post=getOutgoingRPCodec(client).getUncorrelatedExchanges().size();
		assertEquals(pre,post);
	}
	
	private MAPCodec getOutgoingRPCodec(BaseWSRFClient client){
		Client wsClient=ClientProxy.getClient(client.getRP());
		for(Interceptor<?> i : wsClient.getOutInterceptors()){
			if(i instanceof MAPCodec)
				return (MAPCodec)i;
		}
		return null;
	}

}
