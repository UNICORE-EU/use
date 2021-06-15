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
 

package eu.unicore.services.ws;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument.TerminationTime;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertType;
import org.oasisOpen.docs.wsrf.rp2.QueryExpressionType;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.ws.impl.WSRFRepresentation;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;
import eu.unicore.services.ws.impl.WSResourceImpl;
import junit.framework.TestCase;


/**
 * some tests for the WsResourceImpl
 * 
 * @author schuller
 */
public class TestWsResourceImpl extends TestCase {
	
	protected void setUp(){

	}
	
	public void test2()throws Exception{
		WSResourceImpl ws=new WSResourceImpl(){
			@Override
			public QName getResourcePropertyDocumentQName() {
				return null;
			}
		};
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		assertNotNull(ws.getRenderer(WSServerResource.RPcurrentTimeQName));
		XmlObject[]rps=ws.getResourcePropertyXML(WSServerResource.RPcurrentTimeQName);
		assertTrue(rps.length==1);
		assertNotNull(ws.getRenderer(WSServerResource.RPterminationTimeQName));
	}

	public void testPartialGetXML()throws Exception{
		WSResourceImpl ws=new WSResourceImpl(){
			@Override
			public QName getResourcePropertyDocumentQName() {
				return null;
			}
		};
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		XmlRenderer rp=ws.getRenderer(WSServerResource.RPcurrentTimeQName);
		assertNotNull(rp);
		XmlObject[]rps=rp.render();
		assertEquals(1,rps.length);
		List<XmlObject>partial=rp.render(0,1);
		assertEquals(1,partial.size());
		
		//can request more than the actual length
		partial=rp.render(0,10);
		assertEquals(1,partial.size());
		
		try{
			//can not specify offset that is beyond the actual length
			partial=rp.render(1,1);
			fail("Expected exception here.");
		}
		catch(IndexOutOfBoundsException ok){}
	}
	
	public void testRenderPartialQNameSet()throws Exception{
		WSResourceImpl ws=new WSResourceImpl(){};
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		WSRFRepresentation rep=new WSRFRepresentation(ws);
		XmlObject o=rep.getContentObject();
		assertTrue(o.toString().contains("CurrentTime"));
		assertTrue(o.toString().contains("TerminationTime"));
		
		Set<QName>qnames=new HashSet<QName>();
		qnames.add(WSServerResource.RPcurrentTimeQName);
		rep=new WSRFRepresentation(ws, qnames);
		o=rep.getContentObject();
		assertTrue(o.toString().contains("CurrentTime"));
		assertFalse(o.toString().contains("TerminationTime"));
	}

	public void testConditionalGet()throws Exception{
		WSResourceImpl ws=new WSResourceImpl(){
			@Override
			public QName getResourcePropertyDocumentQName() {
				return null;
			}
		};
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		WSRFRepresentation rep=new WSRFRepresentation(ws);
		String etag=rep.getETag();
		System.out.println("Etag: "+etag);
		assertNotNull(etag);
		XmlObject o=rep.getContentObject();
		assertTrue(o.toString().contains("CurrentTime"));
		assertTrue(o.toString().contains("TerminationTime"));
		
		o=rep.conditionalGet(etag);
		assertFalse(o.toString().contains("CurrentTime"));
		assertFalse(o.toString().contains("TerminationTime"));
		
		o=rep.conditionalGet("123");
		assertTrue(o.toString().contains("CurrentTime"));
		assertTrue(o.toString().contains("TerminationTime"));
	}
	
	public void testSetTerminationTime() throws Exception {
		SetTerminationTimeDocument d=SetTerminationTimeDocument.Factory.newInstance();
		Calendar c=new GregorianCalendar();
		c.add(Calendar.YEAR,1);
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		WSResourceImpl ws=new MockWSResourceImpl();
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		d.addNewSetTerminationTime().setRequestedTerminationTime(c);
		ws.SetTerminationTime(d);
		//get tt
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSServerResource.RPterminationTimeQName);
		GetResourcePropertyResponseDocument res=ws.GetResourceProperty(req);
		assertNotNull(res);
		System.out.println(res);
		TerminationTime tt=((TerminationTimeDocument)WSUtilities.extractAnyElements(res, ResourceLifetime.RPterminationTimeQName)[0]).
                getTerminationTime();
		assertNotNull(tt);
		assertEquals(tt.getCalendarValue().getTimeInMillis(),
				c.getTimeInMillis());
	}

	public void testSetNullTerminationTime() throws Exception {
		SetTerminationTimeDocument d=SetTerminationTimeDocument.Factory.newInstance();
		d.addNewSetTerminationTime().setNilRequestedTerminationTime();
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		WSResourceImpl ws=new MockWSResourceImpl();
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		ws.SetTerminationTime(d);
		//get tt
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSServerResource.RPterminationTimeQName);
		GetResourcePropertyResponseDocument res=ws.GetResourceProperty(req);
		assertNotNull(res);
		TerminationTime tt=((TerminationTimeDocument)WSUtilities.extractAnyElements(res, ResourceLifetime.RPterminationTimeQName)[0]).
                getTerminationTime();
		assertNotNull(tt);
		System.out.println(res);
	}

	public void testGetRPDoc() throws Exception {
		WSResourceImpl ws=new MockWSResourceImpl();
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setKernel(kernel);
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.initialise(new InitParameters());
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSServerResource.RPcurrentTimeQName);
		GetResourcePropertyDocumentResponseDocument res=ws.GetResourcePropertyDocument(null);
		assertNotNull(res);
		assertTrue(res.toString().contains("data"));
		System.out.println(res);
	}
	
	public void testGetRP1() throws Exception {
		WSResourceImpl ws=new MockWSResourceImpl();
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setKernel(kernel);
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.initialise(new InitParameters());
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSServerResource.RPcurrentTimeQName);
		GetResourcePropertyResponseDocument res=ws.GetResourceProperty(req);
		assertNotNull(res);
		assertTrue(res.getGetResourcePropertyResponse().toString().contains("CurrentTime"));
	}
	
	public void testGetRP2() throws Exception {
		WSResourceImpl ws=new MockWSResourceImpl();
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());

		ws.setKernel(kernel);
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.initialise(new InitParameters());
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSServerResource.RPterminationTimeQName);
		GetResourcePropertyResponseDocument res=ws.GetResourceProperty(req);
		assertNotNull(res);
		assertTrue(res.getGetResourcePropertyResponse().toString().contains("TerminationTime"));
	}
	
	public void testGetMultipleResourceProperties()throws Exception{
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		WSResourceImpl ws=new MockWSResourceImpl();
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		
		QName[] names=new QName[]{
				ResourceLifetime.RPcurrentTimeQName,
				ResourceLifetime.RPterminationTimeQName
		};
		
		GetMultipleResourcePropertiesDocument in=GetMultipleResourcePropertiesDocument.Factory.newInstance();
		in.addNewGetMultipleResourceProperties();
		for(QName q: names){
			in.getGetMultipleResourceProperties().addNewResourceProperty().setQNameValue(q);
		}
		GetMultipleResourcePropertiesResponseDocument res=ws.GetMultipleResourceProperties(in);
		
		XmlObject response=res.getGetMultipleResourcePropertiesResponse();
		for(QName q: names){
			try{
				XmlObject[]rp=WSUtilities.extractAny(response, q);
				assertNotNull(rp);
				assertEquals(1,rp.length);
			}catch(Exception e){
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}
	
	public void testGetMultiRP() throws Exception {
		WSResourceImpl ws=new MockWSResourceImpl();
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSServerResource.RPterminationTimeQName);
		GetResourcePropertyResponseDocument res=ws.GetResourceProperty(req);
		assertNotNull(res);
		assertTrue(res.getGetResourcePropertyResponse().toString().contains("TerminationTime"));
	}
	
	public class MockRenderer extends AbstractXmlRenderer {
		
		public MockRenderer(){
			super(EndpointReferenceDocument.type.getDocumentElementName());
		}
		public XmlObject[] render(){
			EndpointReferenceDocument epr1=EndpointReferenceDocument.Factory.newInstance();
			epr1.addNewEndpointReference().addNewAddress().setStringValue("gagagag");
			EndpointReferenceDocument epr2=EndpointReferenceDocument.Factory.newInstance();
			epr2.addNewEndpointReference().addNewAddress().setStringValue("gagagag");
			
			return new XmlObject[]{epr1,epr2};
		}
		
		@Override
		public void updateDigest(MessageDigest md) throws Exception {
			for(XmlObject o: render()){
				md.update(String.valueOf(o).getBytes());
			}
		}
	}
	
	public class MockWSR extends WSResourceImpl{
		
		public MockWSR(){
			super();
			addRenderer(new QName("a","b"), new MockRenderer());
		}
		
		@Override
		public QName getResourcePropertyDocumentQName() {
			return null;
		}
		
	}
	
	public void testArrayRP()throws Exception{
		WSResourceImpl ws=new MockWSR();
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setKernel(kernel);
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.initialise(new InitParameters());
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(new QName("a","b"));
		GetResourcePropertyResponseDocument resp=ws.GetResourceProperty(req);
		int n=resp.toString().split("gagaga").length;
		assertEquals(3,n); //corresponds to 2 actual entries
	}
	
	
	public void testGetResourcePropertiesDocument()throws Exception{
		SetTerminationTimeDocument d=SetTerminationTimeDocument.Factory.newInstance();
		Calendar c=new GregorianCalendar();
		c.add(Calendar.YEAR,1);
		WSResourceImpl ws=new WSResourceImpl(){
			@Override
			public QName getResourcePropertyDocumentQName() {
				return null;
			}
		};
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setKernel(kernel);
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.initialise(new InitParameters());
		d.addNewSetTerminationTime().setRequestedTerminationTime(c);
		ws.SetTerminationTime(d);
		
		GetResourcePropertyDocumentResponseDocument res=
			ws.GetResourcePropertyDocument(null);
		assertTrue(res.toString().contains("CurrentTime"));
		assertTrue(res.toString().contains("TerminationTime"));
	}
	
	public void testQueryRP()throws Exception {
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		WSResourceImpl ws=new MockWSResourceImpl();
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.setKernel(kernel);
		ws.initialise(new InitParameters());
		QueryResourcePropertiesDocument req=QueryResourcePropertiesDocument.Factory.newInstance();
		QueryExpressionType q=QueryExpressionType.Factory.newInstance();
		q.setDialect(WSServerResource.QUERY_EXPRESSION_DIALECT_XPATH);
		q.newCursor().setTextValue("blahblah");
			
		req.addNewQueryResourceProperties().setQueryExpression(q);
		assertNotNull(ws.QueryResourceProperties(req));
	}
	
	public void testQueryRP2()throws Exception {
		WSResourceImpl ws=new WSResourceImpl(){
			@Override
			public QName getResourcePropertyDocumentQName() {
				return null;
			}
		};
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		ws.setKernel(kernel);
		ws.setHome(new WSResourceHomeImpl(kernel));
		ws.initialise(new InitParameters());
		QueryResourcePropertiesDocument req=QueryResourcePropertiesDocument.Factory.newInstance();
		QueryExpressionType q=QueryExpressionType.Factory.newInstance();
		q.setDialect(WSServerResource.QUERY_EXPRESSION_DIALECT_XPATH);
		q.newCursor().setTextValue("//*:TerminationTime");
		
		req.addNewQueryResourceProperties().setQueryExpression(q);
		XmlObject o=ws.QueryResourceProperties(req);
		String res=WSUtilities.extractElementTextAsString(o);
		assertTrue(res.length()>0);
		
		req=QueryResourcePropertiesDocument.Factory.newInstance();
		q=QueryExpressionType.Factory.newInstance();
		q.setDialect(WSServerResource.QUERY_EXPRESSION_DIALECT_XPATH);
		q.newCursor().setTextValue("//*:CurrentTime");
		req.addNewQueryResourceProperties().setQueryExpression(q);
		res=WSUtilities.extractElementTextAsString(ws.QueryResourceProperties(req));
		assertTrue(res.length()>0);
	}
	
	public void testInsertRP(){
		InsertResourcePropertiesDocument in=InsertResourcePropertiesDocument.Factory.newInstance();
		in.addNewInsertResourceProperties();
		InsertType insert=in.getInsertResourceProperties().addNewInsert();
		TerminationTimeDocument ttd=TerminationTimeDocument.Factory.newInstance();
		ttd.addNewTerminationTime().setCalendarValue(Calendar.getInstance());
		WSUtilities.insertAny(ttd, insert);
		XmlObject[] xo=insert.selectPath(".");
		assertNotNull(xo[0]);
	}
	
}
