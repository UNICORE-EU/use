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


package eu.unicore.services.ws.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesResponseDocument;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.services.ws.WSServerResource;
import eu.unicore.services.ws.testutils.AbstractClientTest;
import eu.unicore.util.httpclient.HttpUtils;

public class TestClient extends AbstractClientTest {
	
	@Test
	public void testGetConnectionStatus()throws Exception{
		assertEquals(url,client.getUrl());
		assertEquals(epr,client.getEPR());
		assertEquals("OK",client.getConnectionStatus());
	}

	@Test
	public void testGetCurrentTime()throws Exception{
		Calendar time=client.getCurrentTime();
		assertNotNull(time);
		System.out.println("Server time: "+time.getTime());
	}

	@Test
	public void testGetTerminationTime()throws Exception{
		Calendar time=client.getTerminationTime();
		assertNotNull(time);
	}

	@Test
	public void testGetRp()throws Exception{
		CurrentTimeDocument currentTime=CurrentTimeDocument.Factory.parse(client.getResourceProperty(WSServerResource.RPcurrentTimeQName));
		Calendar c=currentTime.getCurrentTime().getCalendarValue();
		assertNotNull(c);
	}

	@Test
	public void testQueryRp()throws Exception{
		QueryResourcePropertiesResponseDocument res=client.queryResourceProperties("//*[local-name()='CurrentTime']");
		XmlObject[] obs=WSUtilities.extractAnyElements(res, CurrentTimeDocument.type.getDocumentElementName());
		assertEquals(1, obs.length);
		System.out.println("Query CurrentTime result: "+obs[0]);
	}

	@Test
	public void testGetMulti()throws Exception{
		Map<QName,XmlObject[]>res=client.getMultipleResourceProperties(ResourceLifetime.RPcurrentTimeQName,ResourceLifetime.RPterminationTimeQName);

		assertNotNull(res.get(ResourceLifetime.RPcurrentTimeQName));
		assertNotNull(res.get(ResourceLifetime.RPterminationTimeQName));

		XmlObject[] ct=res.get(ResourceLifetime.RPcurrentTimeQName);
		assertEquals(1,ct.length);
		CurrentTimeDocument ctd=CurrentTimeDocument.Factory.parse(ct[0].newInputStream());
		assertNotNull(ctd);

		XmlObject[] tt=res.get(ResourceLifetime.RPterminationTimeQName);
		assertEquals(1,tt.length);
		TerminationTimeDocument ttd=TerminationTimeDocument.Factory.parse(tt[0].newInputStream());
		assertNotNull(ttd);
	}

	@Test
	public void testDestroy()throws Exception{
		client.destroy();
	}

	@Test
	public void testScheduledDestruction()throws Exception{
		createTestResource();
		Calendar requestTT=new GregorianCalendar();
		requestTT.add(Calendar.MINUTE, 1);
		try {
			Calendar newTT=client.setTerminationTime(requestTT);
			assertEquals(newTT.getTimeInMillis(),requestTT.getTimeInMillis());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testMakeProxy(){
		try{
			client.makeProxy(Object.class);
			fail("Expected exception");
		}
		catch(IllegalArgumentException ie){/* OK*/ }
		catch(Exception e){
			fail();
		}
	}

	@Test
	public void testGetServicesPage()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		System.out.println("Getting "+url);
		HttpClient cl=HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get=new HttpGet(url);
		HttpResponse response=cl.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals(200,status);
		String res=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		assertTrue(res.contains("Available SOAP services"));
		assertTrue(res.contains(url));
	}

	@Test
	public void testGetWSDL()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL)
				+"/serviceFactory";
		System.out.println("Getting WSDL for "+url);
		HttpClient cl=HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get=new HttpGet(url+"?wsdl");
		HttpResponse response=cl.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals(200,status);
		String wsdl=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		assertTrue(wsdl.contains(url));
	}

}
