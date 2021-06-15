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


package eu.unicore.services.ws.testutils;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.soap.MTOM;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument.GetResourcePropertyResponse;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.SetResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.SetResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesResponseDocument;
import org.w3c.dom.Element;

import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.client.ConditionalGetUtil;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.ResourceProperties;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.services.ws.exceptions.InvalidResourcePropertyQNameFault;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;

@MTOM
public class MockServiceImpl implements ResourceProperties {

	/*
	 * echoes some incoming parameters from ws-addressing
	 */
	@Override
	public GetResourcePropertyResponseDocument GetResourceProperty(org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument in)
			throws BaseFault, ResourceUnknownFault,ResourceUnavailableFault,InvalidResourcePropertyQNameFault
	{
		GetResourcePropertyResponseDocument respdoc = GetResourcePropertyResponseDocument.Factory.newInstance();
		GetResourcePropertyResponse res=respdoc.addNewGetResourcePropertyResponse();

		XmlCursor c=res.newCursor();
		c.toFirstContentToken();
		AddressingProperties p=CXFUtils.getAddressingProperties();
		String wsaTo=p.getTo().getValue();
		c.insertElementWithText(new QName("wsa","wsaTo"), wsaTo);
		ReferenceParametersType refParam=p.getToEndpointReference().getReferenceParameters();
		if(refParam!=null){
			for(Object o: refParam.getAny()){
				String text=null;
				if(o instanceof Element){
					Element o1=(Element)o;
					text=o1.getTextContent();
				}
				else if(o instanceof JAXBElement){
					@SuppressWarnings("unchecked")
					JAXBElement<String>je=(JAXBElement<String>)o;
					text=je.getValue();
				}
				else throw BaseFault.createFault("Unexpected reference parameter class: "+o.getClass().getName());

				c.insertElementWithText(new QName("wsa","wsaRefParam"), text);	
			}
		}
		c.dispose();
		return respdoc;
	}

	public static String rpDocContent="test123";
	public static String etag;
	public static Calendar lastMod=Calendar.getInstance();

	public static boolean slowResponse=true;
	/**
	 * sleeps some seconds for testing timeout
	 */
	@Override
	public GetResourcePropertyDocumentResponseDocument GetResourcePropertyDocument(
			GetResourcePropertyDocumentDocument1 in)
					throws BaseFault, ResourceUnknownFault,
					ResourceUnavailableFault
					{
		try{
			if(slowResponse)Thread.sleep(3000);
		}catch(InterruptedException ex){}
		etag=ConditionalGetUtil.Server.md5(rpDocContent);
		ConditionalGetUtil.Server.mustSendData(lastMod, etag);
		GetResourcePropertyDocumentResponseDocument respDoc = GetResourcePropertyDocumentResponseDocument.Factory.newInstance();
		respDoc.addNewGetResourcePropertyDocumentResponse();
		try{
			XmlObject x=XmlObject.Factory.parse("<x:X xmlns:x=\"http://x\">"
					+rpDocContent+"</x:X>");
			WSUtilities.append(x, respDoc);
		}catch(Exception e){
			throw BaseFault.createFault("Error",e);
		}
		return respDoc;

	}

	/**
	 * throws a fault with a well defined message
	 */
	@Override
	public DeleteResourcePropertiesResponseDocument DeleteResourceProperties(
			DeleteResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		throw BaseFault.createFault("Test123");
					}

	public static Exception putRPException;

	public static AtomicInteger putCalls=new AtomicInteger(0);

	/**
	 * throws a fault with a certain type
	 */
	@Override
	public PutResourcePropertyDocumentResponseDocument PutResourcePropertyDocument(
			PutResourcePropertyDocumentDocument1 in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		putCalls.incrementAndGet();
		if(putRPException!=null){
			if(putRPException instanceof RuntimeException){
				throw ((RuntimeException)putRPException);
			}
			if(putRPException instanceof ResourceUnavailableFault){
				throw ((ResourceUnavailableFault)putRPException);
			}
			throw BaseFault.createFault("testing", putRPException);
		}
		PutResourcePropertyDocumentResponseDocument res=PutResourcePropertyDocumentResponseDocument.Factory.newInstance();
		res.addNewPutResourcePropertyDocumentResponse();
		return res;
	}

	@Override
	public InsertResourcePropertiesResponseDocument InsertResourceProperties(
			InsertResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		throw BaseFault.createFault("NOT IMPLEMENTED");
					}


	@Override
	public UpdateResourcePropertiesResponseDocument UpdateResourceProperties(
			UpdateResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		throw BaseFault.createFault("NOT IMPLEMENTED");
					}


	@Override
	public GetMultipleResourcePropertiesResponseDocument GetMultipleResourceProperties(
			GetMultipleResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		throw BaseFault.createFault("NOT IMPLEMENTED");
					}

	@Override
	public QueryResourcePropertiesResponseDocument QueryResourceProperties(
			QueryResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		throw BaseFault.createFault("NOT IMPLEMENTED");
					}

	@Override
	public SetResourcePropertiesResponseDocument SetResourceProperties(
			SetResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault,
					BaseFault
					{
		throw BaseFault.createFault("NOT IMPLEMENTED");
					}

	@Override
	public DeleteChildResourcesResponseDocument DeleteChildResources(
			DeleteChildResourcesDocument in) throws ResourceUnknownFault,
			ResourceUnavailableFault, BaseFault {
		throw BaseFault.createFault("NOT IMPLEMENTED");
	}

}
