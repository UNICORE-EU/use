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


package de.fzj.unicore.wsrflite.xmlbeans;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.w3.x2003.x05.soapEnvelope.Header;
import org.w3.x2005.x08.addressing.EndpointReferenceType;
import org.w3.x2005.x08.addressing.MetadataType;
import org.w3.x2005.x08.addressing.ReferenceParametersType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.fzj.unicore.wsrflite.WSRFConstants;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.util.Log;


/**
 * tools and utilities, mainly for working with endpoint references, soap headers, and WSRF
 * related XML types 
 * 
 * @author schuller
 * @author demuth
 */
public class WSUtilities {

	protected WSUtilities() {
	}

	/**
	 * returns a new globally unique identifier
	 */
	public static String newUniqueID(){
		return UUID.randomUUID().toString();
	}

	/**
	 * converts XmlBeans schematype to QName
	 */
	public static QName toQName(SchemaType type){
		try{
			if(!type.isDocumentType())return null;
			return type.getDocumentElementName();
		}
		catch(Exception e){return null;}
	}

	public static XmlObject[] extractResourceProperty(GetResourcePropertyDocumentResponseDocument res, QName q){
		return res.selectChildren(q);
	}


	/**
	 * appends a XmlObject into another XML document, immediately before the end tag
	 *
	 * @param what - xml to insert
	 * @param toWhere - target xml
	 */
	public static void append(XmlObject what, XmlObject toWhere){
		XmlCursor sourceCurs=what.newCursor();
		sourceCurs.toNextToken();
		XmlCursor targetCurs = toWhere.newCursor();
		targetCurs.toEndDoc();
		targetCurs.toPrevToken();
		sourceCurs.copyXml(targetCurs);
		sourceCurs.dispose();
		targetCurs.dispose();
	}

	public static void append(XmlObject[] what, XmlObject toWhere){
		XmlCursor targetCurs = toWhere.newCursor();
		targetCurs.toEndDoc();
		targetCurs.toPrevToken();
		for(XmlObject source: what){
			XmlCursor sourceCurs=source.newCursor();
			sourceCurs.toNextToken();
			sourceCurs.copyXml(targetCurs);
			sourceCurs.dispose();
		}
		targetCurs.dispose();
	}

	public static void insertAny(XmlObject what, XmlObject toWhere){
		XmlCursor sourceCurs=what.newCursor();
		sourceCurs.toNextToken();
		XmlCursor targetCurs = toWhere.newCursor();
		targetCurs.toNextToken();
		sourceCurs.copyXml(targetCurs);
		sourceCurs.dispose();
		targetCurs.dispose();
	}

	/**
	 * retrieve Xml content by QName (careful, this will not
	 * return XML documents, but XML fragments. If you want documents
	 * use extractAnyElements() instead)
	 * 
	 * @param source XML to extract from
	 * @param q QName of XML to extract
	 * @return XmlObject array
	 */ 
	public static XmlObject[] extractAny(XmlObject source, QName q){
		return source.selectChildren(q);
	}


	/**
	 * extract all XML elements matching the given qname from an XML source 
	 * document. 
	 * 
	 * @param source - the xml fragment
	 * @param q - QName of elements to extract
	 * @return XmlObject[]
	 */
	public static XmlObject[] extractAllMatchingElements(XmlObject source, QName q){
		List<XmlObject>results=new ArrayList<XmlObject>();
		XmlCursor cursor=source.newCursor();
		try{
			while(goToNextElement(cursor, q)){
				XmlObject o=XmlObject.Factory.parse(cursor.newReader());
				results.add(o);
			}
		}catch(Exception ioe){
			//ignored
		}finally{
			cursor.dispose();
		}
		return results.toArray(new XmlObject[results.size()]);
	}



	public static XmlObject[] extractAllChildren(XmlObject xBean) {
		List<XmlObject> results = new ArrayList<XmlObject>(  );

		XmlCursor cursor = xBean.newCursor();
		if(cursor.getName() == null) cursor.toFirstChild();
		try {
			for ( boolean hasNext = cursor.toFirstChild(  ); hasNext; hasNext = cursor.toNextSibling() )
			{
				XmlObject next;
				try {
					next = XmlObject.Factory.parse(cursor.newXMLStreamReader());
				} catch (XmlException e) {
					continue;
				}

				results.add(next);

			}
		}
		finally
		{
			cursor.dispose(  );
		}
		return (XmlObject[]) results.toArray( new XmlObject[0] );
	}



	/**
	 * extracts XML elements of the given qname
	 * @param source - the source XML
	 * @param q - the qname
	 * @return an non-null array. If no elements of the required name exist, the list will be empty
	 */
	public static XmlObject[] extractAnyElements(XmlObject source, QName q){
		List<XmlObject>results=new ArrayList<XmlObject>();
		XmlCursor cursor=null;
		try{
			if(source != null){
				cursor = source.newCursor();
				boolean hasMore = skipToElement(cursor, q);
				while(hasMore){
					XmlObject next = XmlObject.Factory.parse(cursor.newXMLStreamReader());
					QName name = cursor.getName();
					if(q.getNamespaceURI().equals(name.getNamespaceURI()) 
							&& q.getLocalPart().equals(name.getLocalPart())){
						results.add(next);

					}
					hasMore = cursor.toNextSibling(q);
				}
			}
		}
		catch(XmlException xe){
			//what?
		}
		finally{
			if(cursor!=null)cursor.dispose();
		}
		return results.toArray(new XmlObject[results.size()]);
	}
	/**
	 * extract an array of XML elements identified by their qname from an XML source 
	 * document. 
	 * 
	 * @param source - the xml fragment
	 * @param asClass - the XMLbeans class of the elements to extract
	 * @return  a list of XMLBeans objects with the correct runtime type
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T>extractAnyElements(XmlObject source, Class<T>asClass){
		List<T>results=new ArrayList<T>();
		QName q=XmlBeans.typeForClass(asClass).getDocumentElementName();
		XmlCursor cursor=null;
		try{
			if(source != null){
				cursor = source.newCursor();
				skipToElement(cursor, q);
				boolean hasMore =true;
				while(hasMore){
					XmlObject next = XmlObject.Factory.parse(cursor.newXMLStreamReader());
					QName name = cursor.getName();
					if(q.getNamespaceURI().equals(name.getNamespaceURI()) 
							&& q.getLocalPart().equals(name.getLocalPart())){
						results.add((T)next);
					}
					hasMore = cursor.toNextSibling(q);
				}
			}
		}
		catch(XmlException xe){
			//what?
		}
		finally{
			if(cursor!=null)cursor.dispose();
		}
		return results;
	}

	/**
	 * for an element containing an xsd:any element, find the Qname of the xsd:any 
	 */
	public static QName findAnyElementQName(XmlObject o){
		NodeList nodes=o.getDomNode().getChildNodes();
		if(nodes.getLength()>0){
			Node n=nodes.item(0);
			return new QName(n.getNamespaceURI(),n.getLocalName());
		}
		return null;
	}

	/**
	 * extract the text content of an XML element 
	 * 
	 * @param source the xml element
	 * 
	 * @return the text content, or "" if element has no content
	 */
	public static String extractElementTextAsString(XmlObject source){
		XmlCursor c=null;
		try{
			c=source.newCursor();
			while(c.hasNextToken()){
				if(c.toNextToken().equals(TokenType.TEXT)){
					return c.getChars();	
				}
			}
			return "";
		}finally{
			try{
				c.dispose();
			}catch(Exception e){}
		}
	}

	/**
	 * fast-forward the cursor up to the element with the given QName,
	 * returning <code>false</code> if such an element does not exist
	 */
	public static boolean skipToElement(XmlCursor cursor, QName name){
		// walk through element tree in prefix order (root first, then children from left to right)
		if(name.equals(cursor.getName())) return true;
		boolean hasMoreChildren=true;
		int i=0;
		while(hasMoreChildren){
			hasMoreChildren = cursor.toChild(i++);
			if(hasMoreChildren){
				boolean foundInChild = skipToElement(cursor, name);
				if(foundInChild) return true;
				cursor.toParent();
			}
		}
		return false;
	}

	/**
	 * move cursor forward to the next element, even if some xml hierarchies are
	 * traversed
	 */
	public static boolean goToNextElement(XmlCursor cursor, QName name){
		while(cursor.hasNextToken()){
			TokenType tt=cursor.toNextToken();
			if(tt.isStart()){
				if(name.equals(cursor.getName())){
					return true;                    
				}
			}
		}
		return false;
	}

	public static void addHeaders(Header header, XmlObject[] headers){
		for(XmlObject o: headers)insertAny(o,header);
	}

	public static String extractResourceID(EndpointReferenceType epr){
		try{
			return epr.getAddress().getStringValue().split("=")[1];
		}catch(Exception e){
			return null;}
	}

	public static String extractResourceID(String id){
		try{
			return id.split("=")[1];
		}catch(Exception e){return null;}
	}

	public static String extractServiceName(EndpointReferenceType epr){
		try{
			return extractServiceName(epr.getAddress().getStringValue());
		}catch(Exception e){return null;}
	}

	public static String extractServiceName(String url){
		try{
			URI u = new URI(url);
			String[] path = u.getPath().split("/");
			return path[path.length-1];
		}catch(Exception e){return null;}
	}

	/**
	 * validate that the given String value (interpreted as an Integer) 
	 * is in the supplied range
	 * 
	 * @param value - String to be verified
	 * @param minValue - minimum
	 * @param maxValue - maximum
	 * @return <code>true</code> if the value is within the range
	 */
	public static boolean validateIntegerRange(String value, int minValue, int maxValue){
		try{
			if(value==null)return false;
			Integer i=Integer.parseInt(value);
			if(i<minValue || i > maxValue){
				return false;
			}
		}catch(Exception e){
			return false;
		}
		return true;
	}

	/**
	 * add a reference paramenter to the epr
	 */
	public static void addUGSRefparamToEpr(EndpointReferenceType epr, String id){
		addReferenceParameter(epr, WSRFConstants.U6_RESOURCE_ID, id);
	}

	/**
	 * add a reference parameter with the given qname and value
	 */
	public static void addReferenceParameter(EndpointReferenceType epr, QName qname, String value){
		ReferenceParametersType rp=null;
		rp=epr.getReferenceParameters();
		if(rp==null) rp=epr.addNewReferenceParameters();
		XmlCursor n=rp.newCursor();
		n.toFirstContentToken();
		n.beginElement(qname);
		n.insertChars(value);
		n.toNextToken();
		n.dispose();
	}


	public static void addUGSRefparamToEpr(EndpointReferenceType epr){
		addUGSRefparamToEpr(epr,extractResourceID(epr));
	}

	/** 
	 * Extract the InterfaceName (i.e. port tye from the EPRs Metadata element
	 * @param epr to parse
	 * @return the QName of the implemented interface or null if none could be determined
	 */
	public static QName extractInterfaceName(EndpointReferenceType epr) {
		try {
			XmlCursor n=epr.newCursor();
			WSUtilities.skipToElement(n, WSRFConstants.INTERFACE_NAME);
			String localPart = n.getTextValue().substring(n.getTextValue().indexOf(":")+1);
			String prefix = n.getTextValue().substring(0,n.getTextValue().indexOf(":"));
			String namespace = n.namespaceForPrefix(prefix);
			n.dispose();
			return new QName(namespace,localPart);
		} catch (Exception e) {
			return null;
		}
	}

	public static X500Principal extractServerX500Principal(EndpointReferenceType epr){
		String name=extractServerIDFromEPR(epr);
		try {
			return name!=null? X500NameUtils.getX500Principal(name) : null;
		} catch (IOException e) {
			throw new IllegalArgumentException("The server identity is set, but was " +
					"not recognized as a DN: " + e);
		}
	}

	public static String extractServerIDFromEPR(EndpointReferenceType epr){
		try{	
			if(epr==null || epr.getMetadata()==null)return null;
			MetadataType meta=epr.getMetadata();
			XmlObject[] o=WSUtilities.extractAnyElements(meta, WSRFConstants.SERVER_NAME);
			if(o.length==0)return null;
			XmlCursor c=WSUtilities.extractAnyElements(meta, WSRFConstants.SERVER_NAME)[0].newCursor();
			skipToText(c);
			String res=c.getChars();
			c.dispose();
			return res;
		}
		catch(Exception e){}
		return null;
	}

	public static String extractFriendlyNameFromEPR(EndpointReferenceType epr){
		try{	
			if(epr==null || epr.getMetadata()==null)return null;
			MetadataType meta=epr.getMetadata();
			XmlObject[] o=WSUtilities.extractAnyElements(meta, WSRFConstants.FRIENDLY_NAME);
			if(o.length==0)return null;
			XmlCursor c=WSUtilities.extractAnyElements(meta, WSRFConstants.FRIENDLY_NAME)[0].newCursor();
			while(c.toNextToken()!=TokenType.TEXT){}
			String res=c.getChars();
			c.dispose();
			return res;
		}
		catch(Exception e){}
		return null;
	}

	public static void removeFriendlyNameFromEPR(EndpointReferenceType epr){
		try{	
			if(epr==null || epr.getMetadata()==null) return;
			MetadataType meta=epr.getMetadata();
			XmlCursor cursor = meta.newCursor();
			try {
				if(goToNextElement(cursor, WSRFConstants.FRIENDLY_NAME))
				{
					cursor.removeXml();
				}
			} finally {
				cursor.dispose();
			}
		}
		catch(Exception e){}
	}

	/**
	 * create an WSRF endpoint reference, adding a UNICORE reference parameter and the port type (interface name metadata)
	 * 
	 * @param serviceURL - the URL
	 * @param resourceID - the wsrf id
	 * @param portType - the port type
	 * @return EPR 
	 */
	public static EndpointReferenceType makeServiceEPR(String serviceURL, String resourceID, 
			QName portType){
		EndpointReferenceType epr = makeServiceEPR(serviceURL, resourceID);
		addUGSRefparamToEpr(epr, resourceID);
		addPortType(epr, portType);
		return epr;
	}	

	/**
	 * create an endpoint reference for a plain web service, adding the port type (interface name metadata)
	 * 
	 * @param serviceURL - the service URL
	 * @param portType - the port type
	 * @return EPR 
	 */
	public static EndpointReferenceType makeServiceEPR(String serviceURL, QName portType){
		EndpointReferenceType epr = makeServiceEPR(serviceURL);
		addPortType(epr, portType);
		return epr;
	}

	/**
	 * return the epr of the specified ws resource
	 * @param serviceURL
	 * @param resourceID
	 */
	public static EndpointReferenceType makeServiceEPR(String serviceURL, String resourceID){
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(makeServiceAddress(serviceURL, resourceID));
		return epr;
	}

	/**
	 * return the epr of the specified plain ws
	 * @param serviceURL
	 */
	public static EndpointReferenceType makeServiceEPR(String serviceURL){
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(serviceURL);
		return epr;
	}


	/**
	 * builds an wsa Address for the given url + resourceID
	 * 
	 * @param serviceURL
	 * @param resourceID
	 */
	public static String makeServiceAddress(String serviceURL, String resourceID){
		return serviceURL + "?res=" + resourceID;
	}

	/**
	 * add interface name of a service to the metadata of the epr
	 */
	public static void addPortType(EndpointReferenceType epr,QName portType)
	{
		MetadataType meta=epr.getMetadata();
		if(meta==null) meta=epr.addNewMetadata();
		XmlCursor n=meta.newCursor();
		n.toFirstContentToken();
		n.beginElement(WSRFConstants.INTERFACE_NAME);
		n.insertNamespace("x", portType.getNamespaceURI());
		n.insertChars("x:"+portType.getLocalPart());
		n.dispose();
	}

	/**
	 * add the server DN to the metadata of the epr
	 */
	public static void addServerIdentity(EndpointReferenceType epr, String dn)
	{
		MetadataType meta=epr.getMetadata();
		if(meta==null) meta=epr.addNewMetadata();
		XmlCursor n=meta.newCursor();
		n.toFirstContentToken();
		n.beginElement(WSRFConstants.SERVER_NAME);
		n.insertChars(dn);
		n.dispose();
	}

	/**
	 * add the "friendly name" to the metadata of the epr
	 */
	public static void addFriendlyName(EndpointReferenceType epr, String friendlyName)
	{
		MetadataType meta=epr.getMetadata();
		if(meta==null) meta=epr.addNewMetadata();
		XmlCursor n=meta.newCursor();
		n.toFirstContentToken();
		n.beginElement(WSRFConstants.FRIENDLY_NAME);
		n.insertChars(friendlyName);
		n.dispose();
	}

	/**
	 * add the server pubkey in PEM form to the metadata of the epr
	 */
	public static void addServerPublicKey(EndpointReferenceType epr, X509Certificate cert)
	{
		StringWriter out = new StringWriter();
		try(JcaPEMWriter writer = new JcaPEMWriter(out)){
			writer.writeObject(cert);
		}catch(Exception ex){
			Log.logException("Cannot write public key", ex, Log.getLogger("unicore.security", WSUtilities.class));
		}
		addServerPublicKey(epr, out.toString());
	}

	/**
	 * add the server pubkey in PEM form to the metadata of the epr
	 */
	public static void addServerPublicKey(EndpointReferenceType epr, String pem)
	{
		MetadataType meta=epr.getMetadata();
		if(meta==null) meta=epr.addNewMetadata();
		XmlCursor n=meta.newCursor();
		n.toFirstContentToken();
		n.beginElement(WSRFConstants.PUBLIC_KEY);
		n.insertChars(pem);
		n.dispose();
	}

	/**
	 * extract the server pubkey in PEM form from the metadata of the epr
	 */
	public static String extractPublicKey(EndpointReferenceType epr)
	{
		try{	
			if(epr==null || epr.getMetadata()==null)return null;
			MetadataType meta=epr.getMetadata();
			XmlObject[] o=WSUtilities.extractAnyElements(meta, WSRFConstants.PUBLIC_KEY);
			if(o.length==0)return null;
			XmlCursor c=WSUtilities.extractAnyElements(meta, WSRFConstants.PUBLIC_KEY)[0].newCursor();
			skipToText(c);
			String res=c.getChars();
			c.dispose();
			return res;
		}
		catch(Exception e){}
		return null;
	}

	private static void skipToText(XmlCursor c){
		while(c.hasNextToken()){
			TokenType tt = c.toNextToken();
			if(tt==TokenType.TEXT || tt==TokenType.END)break;
		}
	}
	
	/**
	 * convert an XMLBeans EPR to a CXF EPR, adding To and ReferenceParameters elements
	 * @param epr
	 * @return org.apache.cxf.ws.addressing.EndpointReferenceType
	 */
	public static org.apache.cxf.ws.addressing.EndpointReferenceType toCXF(EndpointReferenceType epr){
		org.apache.cxf.ws.addressing.EndpointReferenceType e=new org.apache.cxf.ws.addressing.EndpointReferenceType();

		AttributedURIType uri=new AttributedURIType();
		uri.setValue(epr.getAddress().getStringValue());
		e.setAddress(uri);

		if(epr.getReferenceParameters()!=null){
			XmlObject[]refs=WSUtilities.extractAllChildren(epr.getReferenceParameters());
			if(refs!=null && refs.length>0){
				org.apache.cxf.ws.addressing.ReferenceParametersType refParams=new org.apache.cxf.ws.addressing.ReferenceParametersType();
				int i=0;
				for(XmlObject r: refs){
					try{
						XmlCursor c=epr.getReferenceParameters().newCursor();
						c.toChild(i);
						i++;
						QName name=c.getName();
						c.dispose();
						String content=extractElementTextAsString(r);
						refParams.getAny().add(new JAXBElement<String>(name, String.class, content));
					}catch(Exception ex){}
				}
				e.setReferenceParameters(refParams);
			}
		}

		return e;
	}

	/**
	 * Creates an XmlObject with the given simple string content.
	 * If the class loader is non null, this method will attempt to
	 * make the returned XmlObject having the right runtime type
	 * 
	 * @param qName - the QName of the XML document
	 * @param content - simple String content
	 * @param cl - class loader (may be null)
	 */
	public static XmlObject createXmlDoc(QName qName, String content, ClassLoader cl){
		SchemaType st=null;
		if(cl != null){
			st=XmlBeans.typeLoaderForClassLoader(cl).findDocumentType(qName);
		}
		XmlObject pDoc=XmlObject.Factory.newInstance();
		if(st!=null){
			pDoc.changeType(st);
		}
		XmlCursor c=pDoc.newCursor();
		c.toFirstContentToken();
		c.insertElementWithText(qName, content);
		c.dispose();
		return pDoc;
	}
}
