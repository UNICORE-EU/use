/*********************************************************************************
 * Copyright (c) 2014 Forschungszentrum Juelich GmbH 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.GDate;
import org.apache.xmlbeans.GDateBuilder;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteType;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument.GetMultipleResourcePropertiesResponse;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument.GetResourcePropertyDocumentResponse;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument.GetResourcePropertyResponse;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertType;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryExpressionType;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.SetResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.SetResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateType;
import org.unigrids.services.atomic.types.SecurityDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.InvalidModificationException;
import de.fzj.unicore.wsrflite.exceptions.TerminationTimeChangeRejectedException;
import de.fzj.unicore.wsrflite.exceptions.UnableToSetTerminationTimeException;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.impl.SecuredResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.Modifiable;
import de.fzj.unicore.wsrflite.xmlbeans.WSResource;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.InvalidResourcePropertyQNameFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ACLRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.CurrentTimeRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.SecurityInfoRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.TerminationTimeRenderer;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.wsutil.client.ConditionalGetUtil;
import eu.unicore.services.ws.WSFrontEnd;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * Base class that implements the basic WSRF functions 
 * for resource properties and resource lifetime.
 * When using or subclassing this class, the correct WS interface 
 * name (porttype) and rp document QName need to be provided.
 * 
 * @author schuller
 */
public class WSRFFrontend implements WSFrontEnd, WSResource {
	
	protected static final Logger logger=Log.getLogger(Log.SERVICES,WSRFFrontend.class);
	
	public static final int INSERT=1;
	public static final int DELETE=2;
	public static final int UPDATE=4;
	
	public static final QName RPSecurityInfoProperty=SecurityDocument.type.getDocumentElementName();

	protected final Map<QName,XmlRenderer> renderers = new HashMap<QName, XmlRenderer>();
	
	protected final Resource resource;

	protected final Kernel kernel;

	private EndpointReferenceType myEPR;

	private QName rpDocQName = null;

	private QName portType = null;
	
	public WSRFFrontend(ResourceImpl resource){
		this(resource,null,null);
	}
	
	public WSRFFrontend(ResourceImpl resource,QName docQName,QName portType) {
		this.resource = resource;
		this.kernel = resource.getKernel();
		this.rpDocQName = docQName;
		this.portType = portType;
		addRenderer(new SecurityInfoRenderer(resource));
		addRenderer(new ACLRenderer(resource));
		addRenderer(new CurrentTimeRenderer());
		addRenderer(new TerminationTimeRenderer(resource));
	}
	
	public void addRenderer(XmlRenderer r){
		addRenderer(r.getQName(),r);
	}
	
	public void addRenderer(QName q, XmlRenderer r){
		if(q==null)throw new NullPointerException("BUG: QName is null for XmlRenderer "+r.getClass());
		renderers.put(q, r);
	}

	@Override
	public Resource getResource() {
		return resource;
	}
	
	/**
	 * retrieve the XML for a resource property by QName
	 * 
	 * @param qn the QName of the resource property
	 * @return XmlObject[] the resource property or <code>null</code> if there is no XML for the given QName 
	 */
	public XmlObject[] getResourcePropertyXML(QName qn){
		try{
			XmlRenderer rp=renderers.get(qn);
			if(rp!=null){
				return rp.render();
			}
		}catch(Exception e){
			if(logger.isDebugEnabled()){
				logger.debug("Error retrieving rp "+qn,e);
			}
		}
		return null;
	}

	public XmlRenderer getRenderer(QName qn){
		return renderers.get(qn);
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return rpDocQName;
	}

	public void setResourcePropertyDocumentQName(QName q){
		this.rpDocQName = q;
	}


	@Override
	public QName getPortType() {
		return portType;
	}

	public void setPortType(QName q){
		this.portType = q;
	}

	public Map<QName,XmlRenderer>getRenderers(){
		return renderers;
	}

	/**
	 * get the EPR of this WS-Resource
	 */
	protected synchronized EndpointReferenceType getEPR(){
		if(myEPR==null){
			myEPR=WSServerUtilities.newEPR(resource.getKernel().getContainerSecurityConfiguration());
			myEPR.addNewAddress().setStringValue(WSServerUtilities.makeAddress(
					resource.getServiceName(),resource.getUniqueID(), resource.getKernel().getContainerProperties()));
			QName pt = getPortType();
			if (pt != null){
				WSServerUtilities.addPortType(myEPR, getPortType());
			}
		}
		return myEPR;
	}

	@ConcurrentAccess(allow=false)
	public DestroyResponseDocument Destroy(DestroyDocument in) 
	throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
		try{
			if(resource instanceof SecuredResourceImpl){
				if(!((SecuredResourceImpl)resource).isOwnerLevelAccess()){
					throw new AuthorisationException("Access denied! You have to be the resource's owner or server admin to perform this operation.");
				}
			}
			resource.destroy();
			resource.getHome().destroyResource(resource.getUniqueID());
			DestroyResponseDocument drd=DestroyResponseDocument.Factory.newInstance();
			drd.addNewDestroyResponse();
			return drd;
		}catch(Exception e){
			Log.logException("Error during resource destruction.",e,logger);
			throw ResourceNotDestroyedFault.createFault(e.getMessage());
		}
	}

	@ConcurrentAccess(allow=false)
	public SetTerminationTimeResponseDocument SetTerminationTime(
			SetTerminationTimeDocument in) 
	throws UnableToSetTerminationTimeFault, TerminationTimeChangeRejectedFault, ResourceUnknownFault, ResourceUnavailableFault
	{
		Calendar c=null;
		if(!in.getSetTerminationTime().isNilRequestedTerminationTime()){
			c=in.getSetTerminationTime().getRequestedTerminationTime();
			GDuration d=in.getSetTerminationTime().getRequestedLifetimeDuration();
			if(d!=null){
				c=Calendar.getInstance();
				GDateBuilder b=new GDateBuilder(c);
				b.addGDuration(d);
				GDate date=b.toGDate();
				c.setTime(date.getDate());
			}
			if(c!=null){
				try{
					resource.getHome().setTerminationTime(resource.getUniqueID(), c);
				}catch(UnableToSetTerminationTimeException ex){
					throw UnableToSetTerminationTimeFault.createFault(ex.getMessage());
				}catch(TerminationTimeChangeRejectedException ttcr){
					throw TerminationTimeChangeRejectedFault.createFault(ttcr.getMessage());
				}
			}
			else throw UnableToSetTerminationTimeFault.createFault("Illegal arguments to SetTerminationTime.");
		}
		else{
			//infinite lifetime
			try{
				resource.getHome().setTerminationTime(resource.getUniqueID(), null);
			}catch(UnableToSetTerminationTimeException ex){
				throw UnableToSetTerminationTimeFault.createFault(ex.getMessage());
			}catch(TerminationTimeChangeRejectedException ttcr){
				throw TerminationTimeChangeRejectedFault.createFault(ttcr.getMessage());
			}
		}
		//make the response doc...
		SetTerminationTimeResponseDocument response=SetTerminationTimeResponseDocument.Factory.newInstance();
		response.addNewSetTerminationTimeResponse().setNewTerminationTime(c);
		response.getSetTerminationTimeResponse().setCurrentTime(Calendar.getInstance());
		return response;
	}

	@SuppressWarnings("rawtypes")
	@ConcurrentAccess(allow=false)
	public DeleteResourcePropertiesResponseDocument DeleteResourceProperties(
			DeleteResourcePropertiesDocument in) throws  ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		try{
			DeleteType delete=in.getDeleteResourceProperties().getDelete();
			QName q = delete.getResourceProperty();
			XmlRenderer rp=renderers.get(q);
			if(!isModifyable(rp, DELETE))throw BaseFault.createFault("Unable to modify <"+q+">");
			((Modifiable)rp).delete();
			DeleteResourcePropertiesResponseDocument res=DeleteResourcePropertiesResponseDocument.Factory.newInstance();
			res.addNewDeleteResourcePropertiesResponse();
			return res;
		}
		catch(InvalidModificationException ime){
			throw BaseFault.createFault("Modifications are not valid.", ime, true);	
		}
	}

	@ConcurrentAccess(allow=false)
	public PutResourcePropertyDocumentResponseDocument PutResourcePropertyDocument(
			PutResourcePropertyDocumentDocument1 in)throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		throw BaseFault.createFault("Not implemented.");
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	@ConcurrentAccess(allow=false)
	public InsertResourcePropertiesResponseDocument InsertResourceProperties(
			InsertResourcePropertiesDocument in) throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		try{
			InsertType insert=in.getInsertResourceProperties().getInsert();
			QName q=WSUtilities.findAnyElementQName(insert);
			XmlObject[] rpChange=WSUtilities.extractAnyElements(insert, q);
			XmlRenderer rp=renderers.get(q);
			if(!isModifyable(rp, INSERT))throw BaseFault.createFault("Unable to modify <"+q+">");
			for(XmlObject o: rpChange){
				((Modifiable)rp).insert(o);
			}
			InsertResourcePropertiesResponseDocument res=InsertResourcePropertiesResponseDocument.Factory.newInstance();
			res.addNewInsertResourcePropertiesResponse();
			return res;
		}
		catch(InvalidModificationException ime){
			throw BaseFault.createFault("Modifications are not valid.", ime, true);	
		}
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	@ConcurrentAccess(allow=false)
	public UpdateResourcePropertiesResponseDocument UpdateResourceProperties(
			UpdateResourcePropertiesDocument in) throws BaseFault {
		try{
			UpdateType update=in.getUpdateResourceProperties().getUpdate();
			QName q=WSUtilities.findAnyElementQName(update);
			if(q==null){
				throw BaseFault.createFault("Invalid update request, qname is null.");
			}
			XmlObject[] rpChange=WSUtilities.extractAnyElements(update, q);
			List<XmlObject> rpChangeAsList = new ArrayList<XmlObject>();
			Collections.addAll(rpChangeAsList, rpChange);
			XmlRenderer rp=renderers.get(q);
			if(!isModifyable(rp, UPDATE))
				throw BaseFault.createFault("Unable to modify <"+q+"> as it is immutable.");
			((Modifiable)rp).update(rpChangeAsList);
			UpdateResourcePropertiesResponseDocument res=UpdateResourcePropertiesResponseDocument.Factory.newInstance();
			res.addNewUpdateResourcePropertiesResponse();
			return res;
		}
		catch(InvalidModificationException ime){
			throw BaseFault.createFault("Modifications are not valid.", ime, true);	
		}
	}

	
	@ConcurrentAccess(allow=true)
	public GetResourcePropertyResponseDocument GetResourceProperty(
			GetResourcePropertyDocument in) throws BaseFault,ResourceUnknownFault, ResourceUnavailableFault,InvalidResourcePropertyQNameFault{
		QName qn=in.getGetResourceProperty();
		if(qn==null) throw InvalidResourcePropertyQNameFault.createFault("Unknown resource property <null>");
		
		XmlObject[] os=getResourcePropertyXML(qn);
		if(os==null) throw InvalidResourcePropertyQNameFault.createFault("Unknown resource property "+qn);
		
		try {
			GetResourcePropertyResponseDocument response=GetResourcePropertyResponseDocument.Factory.newInstance();
			GetResourcePropertyResponse r=response.addNewGetResourcePropertyResponse();
			XmlCursor c=r.newCursor();
			c.toFirstContentToken();
			for(XmlObject o:os){
				if(o== null) continue;
				XmlCursor c1=o.newCursor();
				c1.toFirstContentToken();
				c1.copyXml(c);
			}
			return response;
		} catch (Exception e) {
			Log.logException("Could not build resourceproperty document.",e,logger);
			throw BaseFault.createFault("Could not build resourceproperty document.", e, true);
		}
	}

	@ConcurrentAccess(allow=true)
	public GetMultipleResourcePropertiesResponseDocument GetMultipleResourceProperties(
			GetMultipleResourcePropertiesDocument in)throws BaseFault {
		GetMultipleResourcePropertiesResponseDocument res=GetMultipleResourcePropertiesResponseDocument.Factory.newInstance();
		GetMultipleResourcePropertiesResponse response=res.addNewGetMultipleResourcePropertiesResponse();
		try{
			for(QName q: in.getGetMultipleResourceProperties().getResourcePropertyArray()){
				XmlObject[] rp=getResourcePropertyXML(q);
				if(rp==null)throw new Exception("No content for resource property "+q);
				WSUtilities.append(rp, response);
			}
		
		}catch(Exception e){
			Log.logException("Could not fulfil GetMultipleResourceProperties request.",e,logger);
			throw  BaseFault.createFault("Could not fulfil GetMultipleResourceProperties request.", e, true);
		}
		return res;
	}
	
	/**
	 * Retrieve all the resource properties. This will return the RPs as an "array" of xml.
	 * In general, this will not correspond to the schema doc. If you need schema-compliance,
	 * you'll have to override the protected method 
	 * getResourcePropertyDocument() 
	 * in your WSResource implementation class
	 */
	@ConcurrentAccess(allow=true)
	public GetResourcePropertyDocumentResponseDocument GetResourcePropertyDocument(GetResourcePropertyDocumentDocument1 in)
		throws  BaseFault,ResourceUnknownFault, ResourceUnavailableFault{
		GetResourcePropertyDocumentResponseDocument resDoc=
			GetResourcePropertyDocumentResponseDocument.Factory.newInstance();
		try{
			GetResourcePropertyDocumentResponse res=resDoc.addNewGetResourcePropertyDocumentResponse();
			res.set(getResourcePropertyResponseDocument());
			return resDoc;
		} catch (Exception e) {
			Log.logException("Could not build resourceproperty document.",e,logger);
			throw BaseFault.createFault("Could not build resourceproperty document.", e, true);
		}
	}


	/**
	 * Query resource properties.
	 * As per the wsrf spec, we support only XPath, without needing to
	 * announce the query expression dialect as an rp. 
	 */
	@ConcurrentAccess(allow=true)
	public QueryResourcePropertiesResponseDocument QueryResourceProperties(
			QueryResourcePropertiesDocument in) throws BaseFault{
		
		QueryExpressionType q=in.getQueryResourceProperties().getQueryExpression();
		String dialect = q.getDialect();
		if(!QUERY_EXPRESSION_DIALECT_XPATH.equals(dialect)){
			throw BaseFault.createFault("Unsupported query dialect.");
		}
		try{
			String query=in.getQueryResourceProperties().getQueryExpression().newCursor().getTextValue();
			XmlObject doc=null;
			doc=getResourcePropertyResponseDocument();
			logger.debug("Executing XPath query: "+query+ " against "+doc);
			XmlObject[] os=doc.selectPath(query);
			XmlObject obj=XmlObject.Factory.newInstance();
			if(os!=null){
				logger.debug("Found "+os.length+" results.");
				XmlCursor c=obj.newCursor();
				c.toNextToken();
				for(XmlObject o:os){
					XmlCursor c1=o.newCursor();
					c1.copyXml(c);
					c1.dispose();
				}
			}
			QueryResourcePropertiesResponseDocument res=QueryResourcePropertiesResponseDocument.Factory.newInstance();
			if(os!=null && os.length>0)res.addNewQueryResourcePropertiesResponse().set(obj);
			else res.addNewQueryResourcePropertiesResponse();
			return res;
		}catch(Exception e){
			Log.logException("Error performing query",e,logger);
			throw BaseFault.createFault("Error performing query", e, true);
		}
	}
	
	
	@ConcurrentAccess(allow=false)
	public SetResourcePropertiesResponseDocument SetResourceProperties(
			SetResourcePropertiesDocument in) throws BaseFault{
		throw BaseFault.createFault("Not implemented.");
	}

	@ConcurrentAccess(allow=false)
	public DeleteChildResourcesResponseDocument DeleteChildResources(
			DeleteChildResourcesDocument in) throws BaseFault{
		try{
			DeleteChildResourcesResponseDocument d = DeleteChildResourcesResponseDocument.Factory.newInstance();
			d.addNewDeleteChildResourcesResponse();
			resource.deleteChildren(Arrays.asList(in.getDeleteChildResources().getChildArray()));
			return d;
		}
		catch(Exception e){
			Log.logException("Error deleting children",e,logger);
			throw BaseFault.createFault("Error deleting children", e, true);
		}
	}
	
	/**
	 * returns the resource property document
	 */
	public XmlObject getResourcePropertyResponseDocument() throws Exception {
		String clientETag=ConditionalGetUtil.Server.getIfNoneMatch();
		Calendar ifNotModified=ConditionalGetUtil.Server.getIfModifiedSince();
		boolean hasChangedTimeCheck=hasChangedAfter(ifNotModified);
		
		if(!hasChangedTimeCheck){
			return new WSRFRepresentation(this).getEmptyContentObject();
		}
		else{
			return new WSRFRepresentation(this).conditionalGet(clientETag);
		}
	}
	
	/**
	 * support conditional get: check if the resource has changed after the 
	 * given instant. The default impl always returns <code>true</code>, 
	 * so the next-level check (etags match) is invoked
	 * 
	 * @param instant - may be null
	 */
	protected boolean hasChangedAfter(Calendar instant){
		return true;
	}


	/**
	 * check whether the given rp is modifyable using the given operation
	 * @param rp
	 * @param operation constant, e.g. INSERT
	 */
	protected boolean isModifyable(XmlRenderer rp, int operation){
		if(!(rp instanceof Modifiable))return false;
		if(! ((Modifiable<?>)rp).checkPermissions(operation))return false;
		return true;
	}
}