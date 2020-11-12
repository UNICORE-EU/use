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

package de.fzj.unicore.wsrflite.xmlbeans.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesResponseDocument.QueryResourcePropertiesResponse;
import org.oasisOpen.docs.wsrf.sg2.AddDocument;
import org.oasisOpen.docs.wsrf.sg2.AddResponseDocument;
import org.oasisOpen.docs.wsrf.sg2.ContentType;
import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.oasisOpen.docs.wsrf.sg2.ServiceGroupEntryRPDocument;
import org.oasisOpen.docs.wsrf.sg2.ServiceGroupRPDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.WSRFConstants;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.sg.ServiceGroupRegistration;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Client for accessing a Registry / ServiceGroup service.
 * 
 * Allows to add entries, and to list services based on porttype and filtering criteria.
 * 
 * To allow service discovery, services have to publish their EPR in a special form
 * containing a metadata item. 
 * 
 * <code>
 * EndpointReferenceType epr= ... your service epr... ;
 * WSServerUtilities.addPortType(... your service porttype ...) ;
 * registryClient.addRegistryEntry(epr);
 * </code>
 * 
 * You can add more content to the registry entry, for example resource properties.
 * 
 * @author demuth
 * @author schuller
 */
public class RegistryClient extends BaseWSRFClient implements IRegistryQuery{
	
	private static final Logger logger=Log.getLogger(Log.CLIENT,RegistryClient.class);
	
	public RegistryClient(String url,EndpointReferenceType address, IClientConfiguration sec) throws Exception{
		super(url, address,sec);
	} 
	
	public RegistryClient(EndpointReferenceType address, IClientConfiguration sec) throws Exception{
		super(address,sec);
	} 

	/**
	 * returns the registry's ResourceProperties document
	 */
	public ServiceGroupRPDocument getResourcePropertiesDocument()throws Exception{
		GetResourcePropertyDocumentResponseDocument grprd=GetResourcePropertyDocumentResponseDocument.
		Factory.parse(getResourcePropertyDocument());
		ServiceGroupRPDocument doc=	ServiceGroupRPDocument.Factory.
		parse(grprd.getGetResourcePropertyDocumentResponse().newInputStream());
		return doc;
	}
	
	/**
	 * add a registry entry
	 * @param in
	 * @throws Exception
	 */
	public AddResponseDocument addRegistryEntry(AddDocument in) throws Exception{
		ServiceGroupRegistration reg=makeProxy(ServiceGroupRegistration.class);
		AddResponseDocument res=reg.Add(in);
		return res;
	}
	
	/**
	 * add an entry to the registry configured for UAS
	 * 
	 * @param memberEpr the epr of the service
	 * @param content The content document (ResourceProperty doc)
	 * @throws Exception
	 * @return the epr of the new sg entry
	 */
	public AddResponseDocument addRegistryEntry(EndpointReferenceType memberEpr,
			ContentType content) throws Exception{
	
		AddDocument in=AddDocument.Factory.newInstance();
		in.addNewAdd().setContent(content);
		in.getAdd().setMemberEPR(memberEpr);
		return addRegistryEntry(in);
	}
	
	/**
	 * add an entry to the registry (with no content)
	 * 
	 * @param memberEpr
	 * @throws Exception
	 */
	public AddResponseDocument addRegistryEntry(EndpointReferenceType memberEpr) throws Exception{
		ContentType content=ContentType.Factory.newInstance();
		content.setNil();
		return addRegistryEntry(memberEpr,content);
	}
	
	public AddResponseDocument addRegistryEntry(String endpoint, Map<String,String>content) throws Exception {
		return addRegistryEntry(makeAddRequest(endpoint, content));
	}
	
	/**
	 * make content suitable for publishing from an array of xml elements   
	 * @param os
	 */
	public static ContentType makeContent(XmlObject[] os){
		ContentType content=ContentType.Factory.newInstance();
		XmlCursor c=content.addNewRPDoc().newCursor();
		c.toNextToken();
		for(XmlObject o:os){
			XmlCursor c1=o.newCursor();
			c1.toNextToken();
			c1.copyXml(c);
			c1.dispose();
		}
		return content;
	}

	/**
	 * make content suitable for publishing from a list of xml elements
	 * @param os
	 */
	public static ContentType makeContent(List<XmlObject>os){
		return makeContent(os.toArray(new XmlObject[os.size()]));
	}
	
	public List<EndpointReferenceType> listServices(QName porttype) throws Exception{
		return listServices(porttype,null);
	}
	
	
	public List<EndpointReferenceType> listServices(QName porttype, ServiceListFilter acceptFilter) throws Exception{
		QName child=new QName(ServiceGroupEntryRPDocument.type.getDocumentElementName().getNamespaceURI()
				,"MemberServiceEPR");
		
		String xpath="declare namespace sg='"+ServiceGroupEntryRPDocument.type.getDocumentElementName().getNamespaceURI()+"' ; \n"
		+"declare namespace add='"+WSRFConstants.EPR_METADATA.getNamespaceURI()+"';\n"
		+"declare namespace meta='"+WSRFConstants.INTERFACE_NAME.getNamespaceURI()+"';\n"
		+".//sg:ServiceGroupRP/sg:Entry"
		+"/sg:MemberServiceEPR/add:"+WSRFConstants.EPR_METADATA.getLocalPart()+"/meta:"+WSRFConstants.INTERFACE_NAME.getLocalPart()+"[matches(.,'"+porttype.getLocalPart()+"[ .*]?$')]"
		+"/ancestor::sg:Entry";
		
		QueryResourcePropertiesResponse res=queryResourceProperties(xpath).getQueryResourcePropertiesResponse();
		List<EndpointReferenceType>result=new ArrayList<EndpointReferenceType>();
		try{
			for(XmlObject o: WSUtilities.extractAllMatchingElements(res, child)){
				EntryType entry=EntryType.Factory.parse(o.newInputStream());
				result.add(entry.getMemberServiceEPR());
			}
		}catch(Exception e){
			Log.logException("Registry content parse error.",e,logger);
		}
		
		return result;
	}
	
	public List<EndpointReferenceType> listAccessibleServices(QName porttype)throws Exception{
		return listServices(porttype, new PingWSRFServicesFilter());
	}
	
	public List<EntryType> listEntries()throws Exception{
		EntryType[] entries=getResourcePropertiesDocument().getServiceGroupRP().getEntryArray();
		return Arrays.asList(entries);
	}
	

	/**
	 * checks whether a (WSRF) service can be accessed using the current
	 * security properties
	 */
	public class PingWSRFServicesFilter implements ServiceListFilter {
		public boolean accept(EntryType entry){
			
			try{
				IClientConfiguration sp = RegistryClient.this.getSecurityConfiguration();
				BaseWSRFClient c=new BaseWSRFClient(
						entry.getMemberServiceEPR().getAddress().getStringValue(),
						entry.getMemberServiceEPR(),
						sp);
				c.getCurrentTime();
				return true;	
			}catch(Exception e){
				if(logger.isTraceEnabled()){
					logger.trace("",e);
				}
				return false;
			}
		}
	}
	
	// constants for storing info in the content map
	public static final String ENDPOINT = "Endpoint";
	public static final String INTERFACE_NAME = "InterfaceName";
	public static final String INTERFACE_NAMESPACE = "InterfaceNamespace";
	public static final String SERVER_IDENTITY = "ServerIdentity";
	public static final String SERVER_PUBKEY = "ServerPublicKey";
	
	public static AddDocument makeAddRequest(String endpoint, Map<String,String>content){
		AddDocument ad = AddDocument.Factory.newInstance();
		EndpointReferenceType memberEPR = ad.addNewAdd().addNewMemberEPR();
		memberEPR.addNewAddress().setStringValue(endpoint);
		QName port = new QName(content.get(INTERFACE_NAMESPACE), content.get(INTERFACE_NAME));
		String dn = content.get(SERVER_IDENTITY);
		WSUtilities.addPortType(memberEPR, port);
		WSUtilities.addServerIdentity(memberEPR, dn);
		String pem = content.get(SERVER_PUBKEY);
		if(pem!=null){
			WSUtilities.addServerPublicKey(memberEPR, pem);
		}
		ad.getAdd().addNewContent();
		return ad;
	}
}
