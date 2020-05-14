/*********************************************************************************
 * Copyright (c) 2006-2014 Forschungszentrum Juelich GmbH 
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
 

package eu.unicore.services.ws.impl;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;
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
import org.unigrids.services.atomic.types.SecurityDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.InvalidResourcePropertyQNameFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import eu.unicore.services.ws.WSFrontEnd;
import eu.unicore.services.ws.WSServerResource;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * Combines {@link WSFrontEnd} and {@link ResourceImpl} and is a convenient base class
 * for implementing WSRF services. If you want to provide multiple interfaces to
 * a service (e.g. an additional RESTful interface), it might be better to not use this class,
 * but provide separate frontend and resource classes.
 * 
 * @author schuller
 */
public abstract class WSResourceImpl extends ResourceImpl implements WSServerResource, WSFrontEnd {
	
	protected static final Logger logger=Log.getLogger(Log.SERVICES,WSResourceImpl.class);
	
	public static final QName RPSecurityInfoProperty=SecurityDocument.type.getDocumentElementName();

	/**
	 * if set to Boolean.TRUE, the SecurityInfo resource property will include the server certificate
	 * (if available) 
	 */
	public static final String INITPARAM_SHOW_SERVERCERT_IN_RP = WSResourceImpl.class.getName()+".addServerCert";

	protected WSRFFrontend frontendDelegate;

	public WSResourceImpl() {
		super();
		frontendDelegate = new WSRFFrontend(this);
		frontendDelegate.setResourcePropertyDocumentQName(getResourcePropertyDocumentQName());
		frontendDelegate.setPortType(getPortType());
	}

	public void addRenderer(XmlRenderer r){
		frontendDelegate.addRenderer(r);
	}

	public void addRenderer(QName q, XmlRenderer r){
		frontendDelegate.addRenderer(q,r);
	}
	
	@Override
	public WSResourceImpl getResource(){
		return this;
	}
	
	@Override
	public Map<QName,XmlRenderer> getRenderers(){
		return frontendDelegate.getRenderers();
	}

	public XmlObject[] getResourcePropertyXML(QName qn){
		return frontendDelegate.getResourcePropertyXML(qn);
	}

	public XmlRenderer getRenderer(QName qn){
		return frontendDelegate.getRenderer(qn);
	}
	
	@Override
	public QName getResourcePropertyDocumentQName() {
		return frontendDelegate.getResourcePropertyDocumentQName();
	}

	@ConcurrentAccess(allow=false)
	public DestroyResponseDocument Destroy(DestroyDocument in) 
	throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
		return frontendDelegate.Destroy(in);
	}

	@ConcurrentAccess(allow=false)
	public SetTerminationTimeResponseDocument SetTerminationTime(
			SetTerminationTimeDocument in) 
	throws UnableToSetTerminationTimeFault, TerminationTimeChangeRejectedFault, ResourceUnknownFault, ResourceUnavailableFault
	{
		return frontendDelegate.SetTerminationTime(in);
	}

	@ConcurrentAccess(allow=false)
	public DeleteResourcePropertiesResponseDocument DeleteResourceProperties(
			DeleteResourcePropertiesDocument in) throws  ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		return frontendDelegate.DeleteResourceProperties(in);
	}

	@ConcurrentAccess(allow=false)
	public PutResourcePropertyDocumentResponseDocument PutResourcePropertyDocument(
			PutResourcePropertyDocumentDocument1 in)throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		return frontendDelegate.PutResourcePropertyDocument(in);
	}

	@ConcurrentAccess(allow=false)
	public InsertResourcePropertiesResponseDocument InsertResourceProperties(
			InsertResourcePropertiesDocument in) throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		return frontendDelegate.InsertResourceProperties(in);
	}

	public XmlObject getResourcePropertyResponseDocument() throws Exception {
		return frontendDelegate.getResourcePropertyResponseDocument();
	}
	
	@ConcurrentAccess(allow=true)
	public GetResourcePropertyDocumentResponseDocument GetResourcePropertyDocument(GetResourcePropertyDocumentDocument1 in)
		throws  BaseFault,ResourceUnknownFault, ResourceUnavailableFault{
		return frontendDelegate.GetResourcePropertyDocument(in);
	}

	@ConcurrentAccess(allow=false)
	public UpdateResourcePropertiesResponseDocument UpdateResourceProperties(
			UpdateResourcePropertiesDocument in) throws BaseFault {
		return frontendDelegate.UpdateResourceProperties(in);
	}
	
	@ConcurrentAccess(allow=true)
	public GetResourcePropertyResponseDocument GetResourceProperty(
			GetResourcePropertyDocument in) throws BaseFault,ResourceUnknownFault, ResourceUnavailableFault,InvalidResourcePropertyQNameFault{
		return frontendDelegate.GetResourceProperty(in);
	}

	@ConcurrentAccess(allow=true)
	public GetMultipleResourcePropertiesResponseDocument GetMultipleResourceProperties(
			GetMultipleResourcePropertiesDocument in)throws BaseFault {
		return frontendDelegate.GetMultipleResourceProperties(in);
	}

	@ConcurrentAccess(allow=true)
	public QueryResourcePropertiesResponseDocument QueryResourceProperties(
			QueryResourcePropertiesDocument in) throws BaseFault{
		return frontendDelegate.QueryResourceProperties(in);
	}
	
	@ConcurrentAccess(allow=false)
	public SetResourcePropertiesResponseDocument SetResourceProperties(
			SetResourcePropertiesDocument in) throws BaseFault{
		return frontendDelegate.SetResourceProperties(in);
	}
	
	@ConcurrentAccess(allow=false)
	public DeleteChildResourcesResponseDocument DeleteChildResources(
			DeleteChildResourcesDocument in) throws BaseFault{
		return frontendDelegate.DeleteChildResources(in);
	}
	
	protected EndpointReferenceType getEPR(){
		return frontendDelegate.getEPR();
	}
	
	public QName getPortType()
	{
		return frontendDelegate.getPortType();
	}

}
