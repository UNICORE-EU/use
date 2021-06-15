/*********************************************************************************
 * Copyright (c) 2013 Forschungszentrum Juelich GmbH 
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

package eu.unicore.services.ws.renderers;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlObject;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.ws.AbstractXmlRenderer;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * A property representing a list of service address<br> 
 * 
 * It will automatically adapt to changes of 
 * the server's base URL and identity.
 * 
 * @author schuller
 */
public abstract class AddressListRenderer extends AbstractXmlRenderer{

	private final String serviceName;

	private final QName portType;

	private final boolean addServerIdentity;

	protected final Resource parent;
	
	/**
	 * 
	 * @param serviceName - the parent service name
	 * @param docElementName - the QName of the wrapper element, which must be of type {@link EndpointReferenceType}
	 * @param portType - the port type of the service
	 * @param addServerIdentity - whether to add the server DN in the generated XML
	 */
	public AddressListRenderer(Resource parent, String serviceName, QName docElementName, QName portType, boolean addServerIdentity){
		super(docElementName);
		this.parent=parent;
		this.portType=portType;
		this.serviceName=serviceName;
		this.addServerIdentity=addServerIdentity;
	}

	/**
	 * get the service reference document at the specified index
	 * 
	 * @param index
	 * @return
	 * @throws Exception
	 */
	protected XmlObject getFullAddress(String uid)throws Exception{
		EndpointReferenceType epr=getEPR(uid);
		XmlObject address=makeWrapperDoc(epr);
		return address;
	}

	/**
	 * gets the {@link EndpointReferenceType} for the specified index
	 * @param index
	 */
	protected EndpointReferenceType getEPR(String uid)throws Exception{
		Kernel k=parent.getKernel();
		EndpointReferenceType epr=WSServerUtilities.makeEPR(serviceName,uid,portType,addServerIdentity,k);
		return epr;
	}


	@Override
	public XmlObject[] render() {
		List<String>uids=getUIDs();
		XmlObject[] result=new XmlObject[uids.size()];
		for (int i = 0; i < result.length; i++) {
			try{
				result[i]=getFullAddress(uids.get(i));
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
		return result;
	}

	@Override
	public List<XmlObject> render(int offset, int length)
			throws IndexOutOfBoundsException {
		List<XmlObject>result=new ArrayList<XmlObject>();
		List<String>uids=getUIDs();
		synchronized(uids){
			for (int i = 0; i < length && i+offset<uids.size(); i++) {
				try{
					String id=uids.get(i+offset);
					result.add(getFullAddress(id));
				}catch(Exception ex){
					throw new RuntimeException(ex);
				}
			}
		}
		return result;
	}

	/**
	 * get the UID list from the parent
	 * @return
	 */
	protected abstract List<String>getUIDs();
	
	/**
	 * get the classloader to be used for creating the xml wrapper doc. 
	 * The default implementation just returns getClass().getClassLoader() 
	 * 
	 * @return class loader to be used
	 */
	protected ClassLoader getClassLoader(){
		return getClass().getClassLoader();
	}

	private XmlObject makeWrapperDoc(EndpointReferenceType epr)throws Exception{
		// make the base element
		ClassLoader cl = getClassLoader();
		SchemaType st=XmlBeans.typeLoaderForClassLoader(cl).findDocumentType(qName);
		XmlObject pDoc=XmlObject.Factory.newInstance().changeType(st);
		Method setter=findSetter(pDoc.getClass());
		setter.invoke(pDoc, epr);
		return pDoc;
	}
	
	private Method findSetter(Class<?>cls){
		Method result=null;
		for(Method m: cls.getMethods())
		{
			if(m.getName().startsWith("set")){
				Class<?>[]ps=m.getParameterTypes();
				if(ps.length==1 && EndpointReferenceType.class.isAssignableFrom(ps[0])){
					result=m;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public int getNumberOfElements() throws Exception{
		return getUIDs().size();
	}
	
	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		for(String uid: getUIDs()){
			md.update(uid.getBytes());
		}
		md.update(parent.getKernel().getContainerProperties().getBaseUrl().getBytes());
	}
}
