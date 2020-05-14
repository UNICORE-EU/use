/*********************************************************************************
 * Copyright (c) 2006-2012 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.wsrflite.xmlbeans.client;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryExpressionType;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesDocument.UpdateResourceProperties;
import org.oasisOpen.docs.wsrf.rp2.UpdateType;
import org.unigrids.services.atomic.types.PermitDocument.Permit;
import org.unigrids.services.atomic.types.ShareDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xfire.ClientException;
import de.fzj.unicore.wsrflite.xfire.WSRFClientFactory;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.WSResource;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.InvalidResourcePropertyQNameFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import eu.unicore.security.wsutil.client.ConditionalGetUtil;
import eu.unicore.security.wsutil.client.RetryFeature;
import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ETDClientSettings;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * <b>Overview</b> <br>
 * The basic WSRF client, allowing to access a remote WSRF service that implement one or both
 * of the {@link ResourceProperties} and {@link ResourceLifetime} web service interfaces. 
 * Additionally, this client can be used to create client-side proxies for other 
 * interfaces implemented by the remote service. To do this, use the {@link #makeProxy(Class)} 
 * method with the interface class as a parameter.
 * 
 * <b>Security notes:</b><br>
 * 
 * The client is configured with an instance of {@link IClientConfiguration},
 * which includes SSL, HTTP, trust delegation (ETD) and message signing settings.<br>
 * 
 * To configure a client call including a trust delegation to the server, you need
 * to set at least the ETD receiver and the issuer before creating the client.<br>
 * 
 * <b>Client-side caching and conditional-get notes</b> <br>
 * It is important to try and reduce both the number of web service calls, and the amount of
 * information that is transmitted over the wire. This will improve performance and scalability
 * of both servers and clients. 
 * The WSRF standard offers two basic methods of getting information from the server. On the one
 * hand, the full resource property (RP) document can be retrieved, on the other hand individual parts 
 * of the RP document can be retrieved. The RP document can be big, and can be expensive to compute 
 * on the server side. Thus you should avoid getting the full RP doc unnecessarily. 
 * UNICORE helps you save bandwidth and server-side CPU load with the following optimisations:
 * <ul> 
 * <li>
 *     In UNICORE 7, a "conditional get" was introduced, that has the server compute a checksum (etag)
 *     over the RP doc and send it to the client. Subsequent client calls will send the etag back to
 *     the server, and the server will reply with a brief "not modified" message if the RP doc has not changed.
 *     While this saves bandwidth, the RP doc still has to be computed on the server.
 * </li>
 * <li>
 *     The full RP doc is cached for a short period of time controlled via {@link #setUpdateInterval(long)}. 
 *     By default, the caching time is 500 ms. This is mostly a safeguard against programming errors, like
 *     using the {@link GetResourcePropertyDocument} method in a loop 
 * </li>
 * </ul>
 * 
 * Generally, if you need fast-changing information (like a job status), you should use those methods like
 * {@link #getResourceProperty(QName)} that do not get the full RP doc.
 * 
 * @see WSRFClientFactory
 * @see ETDClientSettings
 *
 * @author schuller
 */
public class BaseWSRFClient {

	protected static final Logger logger=Log.getLogger(Log.CLIENT,BaseWSRFClient.class);

	protected final EndpointReferenceType epr;
	protected final String url;

	protected final WSRFClientFactory proxyMaker;

	protected final ResourceLifetime lt;

	protected final ResourceProperties rp;

	private GetResourcePropertyDocumentResponseDocument resourcePropertyDocument;

	private long updateInterval=500;

	private long lastAccessed;

	// hash over the resource property doc as sent by the server 
	private String etag;

	// last modification time of the resource property doc as sent by the server
	private String lastModified;

	/**
	 * create a Client to connect to service at 'epr'
	 * @param epr the EPR to connect to
	 * @param sec security settings
	 */
	public BaseWSRFClient(EndpointReferenceType epr, IClientConfiguration sec) throws Exception{
		this(epr.getAddress().getStringValue(), epr, sec);
	}

	/**
	 * create a new wsrf client instance
	 * 
	 * @see IClientConfiguration
	 * 
	 * @param endpointUrl the url to connect to
	 * @param epr the service EPR for WS-Addressing
	 * @param sec security settings
	 */
	public BaseWSRFClient(String endpointUrl, EndpointReferenceType epr, 
			IClientConfiguration sec) throws Exception{
		this.epr=epr;
		this.url=endpointUrl;
		if(sec!=null){
			//set TD receiver if necessary and available
			ETDClientSettings etd=sec.getETDSettings();
			if(etd!=null && etd.isExtendTrustDelegation()){
				//we don't want to change the original
				sec=sec.clone();
				setReceiver(epr, sec);
			}
		}
		this.proxyMaker=new WSRFClientFactory(sec);
		lt=makeProxy(ResourceLifetime.class);
		rp=makeProxy(ResourceProperties.class);
	}

	private static void setReceiver(EndpointReferenceType epr, IClientConfiguration sec){
		X500Principal p=WSUtilities.extractServerX500Principal(epr);
		if(p!=null){
			sec.getETDSettings().setReceiver(p);
		}
	}

	/**
	 * create a new wsrf client instance
	 * 
	 * @see WSRFClientFactory
	 * 
	 * @param endpointUrl the url to connect to
	 * @param epr the service EPR for WS-Addressing
	 * @param clientFactory the client factory to use
	 */
	public BaseWSRFClient(String endpointUrl, EndpointReferenceType epr, 
			WSRFClientFactory clientFactory)throws Exception{
		this.epr=epr;
		this.url=endpointUrl;
		this.proxyMaker=clientFactory;
		lt=makeProxy(ResourceLifetime.class);
		rp=makeProxy(ResourceProperties.class);
	}

	/**
	 * clear all cached state to make sure fresh info is obtained from
	 * the server
	 */
	public void clearCache(){
		resourcePropertyDocument=null;
		etag=null;
		lastModified=null;
	}

	/**
	 * get the "friendly name" field of the EPR metadata
	 * 
	 * @return the "friendly name" from the EPR metadata or <code>null</code> if it is not set
	 */
	public final String getFriendlyName(){
		return WSUtilities.extractFriendlyNameFromEPR(epr);
	}

	/**
	 * get the EPR this client points to
	 * @return epr
	 */
	public final EndpointReferenceType getEPR(){
		return epr;
	}

	/**
	 * get the URL this client talks to
	 * @return url
	 */
	public final String getUrl(){
		return url;
	}

	public final IClientConfiguration getSecurityConfiguration() {
		return proxyMaker.getSecurityConfiguration();
	}

	/**
	 * convenience method to create a proxy object for the given interface
	 * If required, the receiver
	 * @param iFace
	 * @throws Exception
	 */
	public <T> T makeProxy(Class<T> iFace) throws Exception{
		if(!iFace.isInterface()){
			throw new IllegalArgumentException("Can only create proxy from an interface.");
		}
		T proxy=proxyMaker.createProxy(iFace,url,epr);
		RetryFeature retry=WSClientFactory.getRetryFeature(proxy);
		retry.getRecoverableExceptions().add(ResourceUnavailableFault.class);
		return proxy;
	}

	/**
	 * Retrieve resource property and return it as a string<br>
	 * Careful: RPs are really arrays, and converting to string makes little sense
	 * In most cases you should use getResourcePropertyXML()
	 * 
	 * @param rpQname - the qname of the resource property
	 * @return the resource property as a String
	 */
	public String getResourceProperty(QName rpQname) 
			throws BaseFault,InvalidResourcePropertyQNameFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		logger.debug("Calling service at wsaTo: "+epr.getAddress().getStringValue());
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(rpQname);
		GetResourcePropertyResponseDocument res=(GetResourcePropertyResponseDocument)rp.GetResourceProperty(req);
		return res.getGetResourcePropertyResponse().toString();
	}

	/**
	 * Retrieve single resource property<br>
	 * Careful: RPs are really arrays, this will retrieve the first one. If you need
	 * the full array, use getResourceProperty() 
	 * 
	 * @param resultDocClass - the Java class (corresponding to the qname) of the desired resource property
	 * @return the resource property or null if the RP is empty
	 */
	public <T> T getSingleResourceProperty(Class<T>resultDocClass) 
			throws BaseFault,InvalidResourcePropertyQNameFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		List<T>res=getResourceProperty(resultDocClass);
		return res.size()>0?getResourceProperty(resultDocClass).get(0):null;
	}

	/**
	 * Get a resource property as a list of the correct runtime type. 
	 * The resource property QName is obtained directly from the XmlBeans document class
	 *  
	 * @param resultDocClass - class of resulting document 
	 * @return array of the correct xmlbeans class
	 */
	public <T> List<T> getResourceProperty(Class<T>resultDocClass)
			throws BaseFault,InvalidResourcePropertyQNameFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		logger.debug("Calling service at wsaTo: "+epr.getAddress().getStringValue());
		QName q=XmlBeans.typeForClass(resultDocClass).getDocumentElementName();
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(q);
		GetResourcePropertyResponseDocument res=(GetResourcePropertyResponseDocument)rp.GetResourceProperty(req);
		return WSUtilities.extractAnyElements(res.getGetResourcePropertyResponse(),resultDocClass);
	}


	/**
	 * destroy a WS-Resource
	 */
	public void destroy() 
			throws BaseFault,ResourceUnavailableFault,ResourceUnknownFault,ResourceNotDestroyedFault,ClientException{
		logger.debug("Calling service at wsaTo: "+epr.getAddress().getStringValue());
		DestroyDocument dd=DestroyDocument.Factory.newInstance();
		dd.addNewDestroy();
		lt.Destroy(dd);
		clearCache();
	}

	/**
	 * set the termination time of a WS-Resource
	 * 
	 * @param newTerminationTime
	 * @return the new termination time
	 */
	public Calendar setTerminationTime(Calendar newTerminationTime) 
			throws BaseFault,UnableToSetTerminationTimeFault,TerminationTimeChangeRejectedFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		logger.debug("Calling service at wsaTo: "+epr.getAddress().getStringValue());
		SetTerminationTimeDocument req=SetTerminationTimeDocument.Factory.newInstance();
		req.addNewSetTerminationTime().setRequestedTerminationTime(newTerminationTime);
		return(lt.SetTerminationTime(req).getSetTerminationTimeResponse().getNewTerminationTime());
	}

	/**
	 * get the termination time of a WS-Resource, which can be null 
	 * if it is not available or set on the server
	 */
	public Calendar getTerminationTime() 
			throws BaseFault,InvalidResourcePropertyQNameFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSResource.RPterminationTimeQName);
		GetResourcePropertyResponseDocument res=(GetResourcePropertyResponseDocument)rp.GetResourceProperty(req);
		try{
			TerminationTimeDocument o=(TerminationTimeDocument)WSUtilities.extractAnyElements(res,ResourceLifetime.RPterminationTimeQName)[0];
			if(o==null)return null;
			if(o.getTerminationTime().isNil())return null;
			return o.getTerminationTime().getCalendarValue();
		}
		catch(Exception ioe){
			throw new ClientException("Could not parse reply from server.",ioe);
		}
	}

	/**
	 * get the current time of a WS-Resource
	 */
	public Calendar getCurrentTime() 
			throws BaseFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(WSResource.RPcurrentTimeQName);
		GetResourcePropertyResponseDocument res=(GetResourcePropertyResponseDocument)rp.GetResourceProperty(req);
		try{
			CurrentTimeDocument o=(CurrentTimeDocument)WSUtilities.extractAnyElements(res,ResourceLifetime.RPcurrentTimeQName)[0];
			return o.getCurrentTime().getCalendarValue();
		}catch(Exception e){
			throw new ClientException("Could not parse reply from server.",e);
		}
	}

	/**
	 * get a string containing the resource properties document, retrieved 
	 * using the {@link GetResourcePropertyDocument} method 
	 */
	public String getResourcePropertyDocument() throws BaseFault,ResourceUnknownFault,ResourceUnavailableFault,ClientException{
		GetResourcePropertyDocumentResponseDocument resp=GetResourcePropertyDocument();
		if(resp==null){
			throw new ClientException("Server <"+getEPR().getAddress().getStringValue()+"> returned <null>");
		}
		return String.valueOf(resp);
	}

	/**
	 * get the resource properties document
	 */
	public GetResourcePropertyDocumentResponseDocument GetResourcePropertyDocument() 
			throws BaseFault,ResourceUnknownFault,ResourceUnavailableFault,ClientException{
		if(resourcePropertyDocument==null || System.currentTimeMillis()-lastAccessed>updateInterval){
			logger.debug("Calling service at wsaTo: "+epr.getAddress().getStringValue());
			GetResourcePropertyDocumentDocument1 req=GetResourcePropertyDocumentDocument1.Factory.newInstance();
			req.addNewGetResourcePropertyDocument();
			ConditionalGetUtil.Client.setIfNoneMatch(etag);
			ConditionalGetUtil.Client.setIfModifiedSince(lastModified);
			GetResourcePropertyDocumentResponseDocument newRP=rp.GetResourcePropertyDocument(req);
			if(!ConditionalGetUtil.Client.isNotModified()){
				resourcePropertyDocument=newRP;
				etag=ConditionalGetUtil.Client.getEtag();
				lastModified=ConditionalGetUtil.Client.getLastModified();
			}
			lastAccessed=System.currentTimeMillis();
		}
		return resourcePropertyDocument;
	}

	/**
	 * perform an xpath query on the resourceproperties
	 */
	public QueryResourcePropertiesResponseDocument queryResourceProperties(String xpath) 
			throws BaseFault,ResourceUnknownFault,ResourceUnavailableFault,ClientException{
		QueryResourcePropertiesDocument req=QueryResourcePropertiesDocument.Factory.newInstance();
		QueryExpressionType q=QueryExpressionType.Factory.newInstance();
		q.setDialect(WSResource.QUERY_EXPRESSION_DIALECT_XPATH);
		q.newCursor().setTextValue(xpath);
		req.addNewQueryResourceProperties().setQueryExpression(q);
		return rp.QueryResourceProperties(req);
	}


	/**
	 * get multiple resource properties
	 * 
	 * @param names - the QNames of the RPs to get
	 * @return a Map mapping the QName to the RP Array
	 */
	public Map<QName,XmlObject[]>getMultipleResourceProperties(QName... names)
			throws BaseFault,ResourceUnknownFault,ResourceUnavailableFault,ClientException{
		if(names==null||names.length==0)throw new IllegalArgumentException("No QNames given.");
		GetMultipleResourcePropertiesDocument in=GetMultipleResourcePropertiesDocument.Factory.newInstance();
		in.addNewGetMultipleResourceProperties();
		for(QName q: names){
			in.getGetMultipleResourceProperties().addNewResourceProperty().setQNameValue(q);
		}
		GetMultipleResourcePropertiesResponseDocument res=rp.GetMultipleResourceProperties(in);

		Map<QName,XmlObject[]>result=new HashMap<QName, XmlObject[]>();
		XmlObject response=res.getGetMultipleResourcePropertiesResponse();
		for(QName q: names){
			try{
				result.put(q, WSUtilities.extractAnyElements(response, q));
			}catch(Exception e){
				logger.warn("Can't extract result for property "+q);
			}
		}
		return result;
	}

	/**
	 * retrieves the minimum time between subsequent 
	 * remote GetResourcePropertyDocument() calls.
	 * (default: 500 ms)<br>
	 * Between calls, the resourceproperty document is cached
	 */
	public long getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * Sets the update interval, i.e. the minimum time between 
	 * subsequent remote GetResourcePropertyDocument() calls. 
	 * Between calls, the resourceproperty document is cached.<br>
	 * To disble the cache, set to a negative value
	 *  
	 * @see #getUpdateInterval()
	 * @param updateInterval
	 */
	public void setUpdateInterval(long updateInterval) {
		this.updateInterval = updateInterval;
	}

	/**
	 * check the connection status to the service
	 * @return a human readable status message
	 */
	public String getConnectionStatus()throws ClientException{
		if(checkConnection())return "OK";
		else return "Can't connect.";
	}	

	/**
	 * check the connection to the service (using the current timeout settings)
	 * @return true if service can be accessed
	 * @throws ClientException
	 */
	public boolean checkConnection()throws ClientException{
		return checkConnection(-1);
	}

	/**
	 * check the connection to the WSRF service by calling getCurrentTime().
	 * If the service does not reply within the given timeout, returns <code>false</code>
	 * 
	 * @param timeout - connection timeout in milliseconds (use negative value to use the current timeout)
	 * @return false in case of problems contacting the remote service
	 * @throws ClientException
	 */
	public boolean checkConnection(int timeout)throws ClientException{
		try{
			if(timeout>0){
				//create a new client with the proper timeout settings
				IClientConfiguration p=getSecurityConfiguration().clone();
				p.getHttpClientProperties().setConnectionTimeout(timeout);
				new BaseWSRFClient(getUrl(), getEPR(), p).getCurrentTime();	
			}
			else{
				getCurrentTime();
			}
			return true;	
		}
		catch(ClientException ce){
			throw ce;
		}
		catch(Exception ex){
			return false;
		}
	}

	public ShareDocument getShares() throws InvalidResourcePropertyQNameFault, BaseFault, 
	ResourceUnavailableFault, ResourceUnknownFault, IOException, XmlException {
		QName rpName = ShareDocument.type.getDocumentElementName();
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(rpName);
		GetResourcePropertyResponseDocument res = rp.GetResourceProperty(req);
		return ShareDocument.Factory.parse(res.getGetResourcePropertyResponse().newReader());
	}

	public void addShare(Permit... aclEntries) throws InvalidResourcePropertyQNameFault, BaseFault, 
	ResourceUnavailableFault, ResourceUnknownFault, IOException, XmlException {
		ShareDocument acl = getShares();
		for(Permit e: aclEntries){
			Permit n = acl.getShare().addNewPermit();
			n.setAllow(e.getAllow());
			n.setWhen(e.getWhen());
			n.setIs(e.getIs());
		}
		updateShare(acl);
	}
	
	public void updateShare(Permit... aclEntries) throws InvalidResourcePropertyQNameFault, BaseFault, 
	ResourceUnavailableFault, ResourceUnknownFault, IOException, XmlException {
		ShareDocument acl = ShareDocument.Factory.newInstance();
		acl.addNewShare();
		for(Permit e: aclEntries){
			Permit n = acl.getShare().addNewPermit();
			n.setAllow(e.getAllow());
			n.setWhen(e.getWhen());
			n.setIs(e.getIs());
		}
		updateShare(acl);
	}
	
	protected void updateShare(ShareDocument newACL) 
			throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault {
		UpdateResourcePropertiesDocument reqDoc = UpdateResourcePropertiesDocument.Factory.newInstance();
		UpdateResourceProperties req = reqDoc.addNewUpdateResourceProperties();
		UpdateType update = req.addNewUpdate();
		update.set(newACL);
		rp.UpdateResourceProperties(reqDoc);
	}
	
	/**
	 * delete the given set of "children" (WS-Resources) of this resource
	 * 
	 * @param childIDs - unique IDs (or full URLs) of the children to delete
	 * @throws ResourceUnavailableFault
	 * @throws ResourceUnknownFault
	 * @throws BaseFault
	 */
	public void deleteChildren(Collection<String>childIDs) throws ResourceUnavailableFault, ResourceUnknownFault, BaseFault {
		DeleteChildResourcesDocument req = DeleteChildResourcesDocument.Factory.newInstance();
		req.addNewDeleteChildResources();
		for(String id: childIDs){
			if(id.contains("?res="))id=WSUtilities.extractResourceID(id);
			req.getDeleteChildResources().addChild(id);
		}
		rp.DeleteChildResources(req);
	}

	/**
	 * get the {@link ResourceProperties} instance for directly using the 
	 * resource properties functionality
	 *  
	 * @return {@link ResourceProperties} interface
	 */
	public ResourceProperties getRP(){
		return rp;
	}

	/**
	 * get the {@link ResourceLifetime} instance for directly using the 
	 * resource lifetime functionality
	 *  
	 * @return {@link ResourceLifetime} interface
	 */
	public ResourceLifetime getLT(){
		return lt;
	}

}
