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


package eu.unicore.services;

import java.util.Calendar;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument.GetMultipleResourcePropertiesResponse;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.ws.WSUtilities;
import junit.framework.TestCase;

public class TestWSUtilities extends TestCase{

	public void testExtractAny()throws Exception{
		CurrentTimeDocument ctd=CurrentTimeDocument.Factory.newInstance();
		ctd.addNewCurrentTime().setCalendarValue(Calendar.getInstance());
		CurrentTimeDocument ctd2=CurrentTimeDocument.Factory.newInstance();
		ctd2.addNewCurrentTime().setCalendarValue(Calendar.getInstance());

		GetMultipleResourcePropertiesResponseDocument r=GetMultipleResourcePropertiesResponseDocument.Factory.newInstance();
		GetMultipleResourcePropertiesResponse res=r.addNewGetMultipleResourcePropertiesResponse();
		WSUtilities.append(new XmlObject[]{ctd,ctd2}, res);
		XmlObject[] extr=WSUtilities.extractAny(res, CurrentTimeDocument.type.getDocumentElementName());
		assertNotNull(extr);
		assertEquals(2,extr.length);
	}

	public void testExtractAnyElementsTyped()throws Exception{
		CurrentTimeDocument ctd=CurrentTimeDocument.Factory.newInstance();
		ctd.addNewCurrentTime().setCalendarValue(Calendar.getInstance());
		CurrentTimeDocument ctd2=CurrentTimeDocument.Factory.newInstance();
		ctd2.addNewCurrentTime().setCalendarValue(Calendar.getInstance());

		GetMultipleResourcePropertiesResponseDocument r=GetMultipleResourcePropertiesResponseDocument.Factory.newInstance();
		GetMultipleResourcePropertiesResponse res=r.addNewGetMultipleResourcePropertiesResponse();
		WSUtilities.append(new XmlObject[]{ctd,ctd2}, res);
		XmlObject[] extr=WSUtilities.extractAnyElements(res, CurrentTimeDocument.type.getDocumentElementName());
		assertNotNull(extr);
		assertEquals(2,extr.length);
		//check we got currenttimedocuments
		assertTrue(extr[0] instanceof CurrentTimeDocument);
	}

	public void testExtractAnyElementsTypeSafe()throws Exception{
		CurrentTimeDocument ctd=CurrentTimeDocument.Factory.newInstance();
		ctd.addNewCurrentTime().setCalendarValue(Calendar.getInstance());
		CurrentTimeDocument ctd2=CurrentTimeDocument.Factory.newInstance();
		ctd2.addNewCurrentTime().setCalendarValue(Calendar.getInstance());

		GetMultipleResourcePropertiesResponseDocument r=GetMultipleResourcePropertiesResponseDocument.Factory.newInstance();
		GetMultipleResourcePropertiesResponse res=r.addNewGetMultipleResourcePropertiesResponse();
		WSUtilities.append(new XmlObject[]{ctd,ctd2}, res);
		List<CurrentTimeDocument> extr=WSUtilities.extractAnyElements(res, CurrentTimeDocument.class);
		assertNotNull(extr);
		assertEquals(2,extr.size());
		CurrentTimeDocument res_ctd=extr.get(0);
		assertTrue(ctd.getCurrentTime().getCalendarValue().getTimeInMillis()
				==res_ctd.getCurrentTime().getCalendarValue().getTimeInMillis());
	}

	public void testValidateKernelProps(){
		assertTrue(WSUtilities.validateIntegerRange("10", 1, 10));
		assertTrue(WSUtilities.validateIntegerRange("1", 1, 10));
		assertFalse(WSUtilities.validateIntegerRange("100", 1, 10));
		//null values are validated as false
		assertFalse(WSUtilities.validateIntegerRange(null, 1, 10));
	}

	public void testExtractServiceName(){
		String url="http://localhost:123/FOO/services/TEST?res=bar";
		assertEquals("TEST", WSUtilities.extractServiceName(url));
		url="xfire.local://localhost:123/FOO/services/TEST?res=bar";
		assertEquals("TEST", WSUtilities.extractServiceName(url));
	}

	public void testExtractXmlElementsUntyped(){
		try{
			StringBuilder sb=new StringBuilder();
			sb.append("<x:Element1 xmlns:x=\"x.org\">");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("</x:Element1>");
			QName q=new QName("y.org","Element2");
			XmlObject o=XmlObject.Factory.parse(sb.toString());
			XmlObject[] os=WSUtilities.extractAnyElements(o, q);
			assertNotNull(os);
			assertEquals(3,os.length);
			assertTrue(os[0].toString().contains("Element2"));
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}

	public void testExtractAllMatchingXmlElements(){
		try{
			StringBuilder sb=new StringBuilder();
			sb.append("<x:Element0 xmlns:x=\"x.org\">");
			sb.append("<x:Element1 xmlns:x=\"x.org\">");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
			sb.append("</x:Element1>");
			sb.append("<x:Element1 xmlns:x=\"x.org\">");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
			sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
			sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
			sb.append("</x:Element1>");
			sb.append("</x:Element0>");
			QName q=new QName("y.org","Element2");
			XmlObject o=XmlObject.Factory.parse(sb.toString());
			XmlObject[] os=WSUtilities.extractAllMatchingElements(o, q);
			assertNotNull(os);
			assertEquals(6,os.length);
			assertTrue(os[0].toString().contains("Element2"));
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}

	public void testNewUniqueID() {
		assertTrue(WSUtilities.newUniqueID().length()>8);
	}

	public void testExtractAllChildren()throws Exception{
		StringBuilder sb=new StringBuilder();
		sb.append("<x:Element0 xmlns:x=\"x.org\">");
		sb.append("<x:Element1 xmlns:x=\"x.org\">");
		sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
		sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
		sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
		sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
		sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
		sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
		sb.append("</x:Element1>");
		sb.append("<x:Element1 xmlns:x=\"x.org\">");
		sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
		sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
		sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
		sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
		sb.append("<y:Element2 xmlns:y=\"y.org\"/>");
		sb.append("<y:Element3 xmlns:y=\"z.org\"/>");
		sb.append("</x:Element1>");
		sb.append("</x:Element0>");

		XmlObject o=XmlObject.Factory.parse(sb.toString());

		XmlObject[] extr=WSUtilities.extractAllChildren(o);
		assertNotNull(extr);
		assertEquals(2,extr.length);
		for(XmlObject child : extr)
		{
			assertEquals(6, WSUtilities.extractAllChildren(child).length);
		}

		CurrentTimeDocument ctd=CurrentTimeDocument.Factory.newInstance();
		ctd.addNewCurrentTime().setCalendarValue(Calendar.getInstance());

		CurrentTimeDocument ctd2=CurrentTimeDocument.Factory.newInstance();
		ctd2.addNewCurrentTime().setCalendarValue(Calendar.getInstance());

		GetMultipleResourcePropertiesResponseDocument r=GetMultipleResourcePropertiesResponseDocument.Factory.newInstance();
		GetMultipleResourcePropertiesResponse res=r.addNewGetMultipleResourcePropertiesResponse();
		WSUtilities.append(new XmlObject[]{ctd,ctd2}, res);
		assertEquals(2, WSUtilities.extractAllChildren(r).length);
		assertEquals(2, WSUtilities.extractAllChildren(res).length);


	}

	public void testExtractAnyElements()throws Exception{
		XmlObject source=XmlObject.Factory.parse("<x xmlns=\"n\">" +
				"<foo>1</foo>" +
				"<foo>2</foo>" +
				"<foo>3</foo>" +
				"</x>");
		QName q=new QName("n","foo");
		XmlObject[] res=WSUtilities.extractAnyElements(source, q);
		assertEquals(3, res.length);
	}

	public void testExtractAnyElements2()throws Exception{
		XmlObject source=XmlObject.Factory.parse("<x xmlns=\"n\">" +
				"<foo xmlns=\"m\"><bar1>213</bar1></foo>" +
				"<foo xmlns=\"m\"><bar2/></foo>" +
				"<foo xmlns=\"m\"><bar3/></foo>" +
				"</x>");
		QName q=new QName("m","foo");
		XmlObject[] res=WSUtilities.extractAnyElements(source, q);
		assertEquals(3, res.length);
	}
	public void testFindAnyElementQName() throws Exception{
		XmlObject source=XmlObject.Factory.parse("<x xmlns=\"n\"/>");
		QName expected=new QName("n","x");
		QName found=WSUtilities.findAnyElementQName(source);
		assertEquals(expected,found);
	}

	public void testAppend()throws Exception{
		XmlObject target=XmlObject.Factory.parse("<x xmlns=\"n\"/>");
		XmlObject what=XmlObject.Factory.parse("<foo xmlns=\"n\">1</foo>");
		QName q=new QName("n","foo");
		WSUtilities.append(what, target);
		XmlObject[] res=WSUtilities.extractAnyElements(target, q);
		assertEquals(1, res.length);
	}

	public void testAppendMany()throws Exception{
		XmlObject target=XmlObject.Factory.parse("<x xmlns=\"n\"/>");
		XmlObject what=XmlObject.Factory.parse("<foo xmlns=\"n\">1</foo>");
		QName q=new QName("n","foo");
		WSUtilities.append(new XmlObject[]{what,what,what}, target);
		XmlObject[] res=WSUtilities.extractAnyElements(target, q);
		assertEquals(3, res.length);
	}

	public void testValidateProps(){
		assertTrue(WSUtilities.validateIntegerRange("10", 1, 10));
		assertTrue(WSUtilities.validateIntegerRange("1", 1, 10));
		assertFalse(WSUtilities.validateIntegerRange("100", 1, 10));
		//null values are validated as false
		assertFalse(WSUtilities.validateIntegerRange(null, 1, 10));
	}

	public void testInsertAndExtractIdentity(){
		String dn="CN=test server";
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://test:123");
		WSUtilities.addServerIdentity(epr, dn);
		assertTrue(epr.toString().contains(dn));
		String dn_extracted=WSUtilities.extractServerIDFromEPR(epr);
		assertEquals(dn, dn_extracted);

		epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://test:456");
		assertNull(WSUtilities.extractServerX500Principal(epr));

	}

	@SuppressWarnings("unchecked")
	public void testConvertEPR()throws Exception{
		EndpointReferenceDocument eprD=EndpointReferenceDocument.Factory.newInstance();
		EndpointReferenceType epr=eprD.addNewEndpointReference();
		String addr="http://foo?res=x1";
		epr.addNewAddress().setStringValue(addr);
		XmlCursor c=epr.addNewReferenceParameters().newCursor();
		c.toFirstContentToken();
		c.insertElementWithText(new QName("http://bar","baz"), "foo123");
		c.insertElementWithText(new QName("http://yum","yum"), "abcd");
		c.dispose();
		System.out.println("Original EPR: ");
		System.out.println(eprD);
		org.apache.cxf.ws.addressing.EndpointReferenceType cxfEPR=WSUtilities.toCXF(epr);
		assertEquals(addr, cxfEPR.getAddress().getValue());

		List<Object>refParams=cxfEPR.getReferenceParameters().getAny();
		assertEquals(2, refParams.size());

		JAXBElement<String>r1=(JAXBElement<String>)refParams.get(0);
		assertEquals("foo123", r1.getValue());

		JAXBElement<String>r2=(JAXBElement<String>)refParams.get(1);
		assertEquals("abcd", r2.getValue());
	}

	public void testExtractServerIDs(){
		String pem="not_really_a_pem";
		String dn = "CN=Server";

		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://foo");
		WSUtilities.addServerPublicKey(epr, pem);
		WSUtilities.addServerIdentity(epr,dn);
		assertEquals(pem, WSUtilities.extractPublicKey(epr));
		assertEquals(dn, WSUtilities.extractServerIDFromEPR(epr));

		epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://foo");
		WSUtilities.addServerPublicKey(epr, pem);
		WSUtilities.addServerIdentity(epr,"");
		assertEquals(pem, WSUtilities.extractPublicKey(epr));
		assertEquals("", WSUtilities.extractServerIDFromEPR(epr));
		
		epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://foo");
		assertNull(WSUtilities.extractPublicKey(epr));
		assertNull(WSUtilities.extractServerIDFromEPR(epr));
				
	}
}
