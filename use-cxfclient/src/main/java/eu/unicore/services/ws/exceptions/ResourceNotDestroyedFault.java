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
 

package eu.unicore.services.ws.exceptions;

import java.util.Calendar;

import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.oasisOpen.docs.wsrf.rl2.ResourceNotDestroyedFaultDocument;
import org.oasisOpen.docs.wsrf.rl2.ResourceNotDestroyedFaultType;

/**
 * the resource could not be destroyed (by calling the Destroy() method)
 * 
 * @author schuller
 */
@WebFault(name="ResourceNotDestroyedFault", targetNamespace="http://docs.oasis-open.org/wsrf/rl-2")
public class ResourceNotDestroyedFault extends RuntimeException implements
		FaultOutInterceptor.FaultInfoException {

	private static final long serialVersionUID = 1L;

	private ResourceNotDestroyedFaultType faultDetail;
	
	public ResourceNotDestroyedFaultType getFaultInfo() {
		return faultDetail;
	}
	
	public static QName getFaultName() {
		return ResourceNotDestroyedFaultDocument.type.getDocumentElementName();
	}
	
	/**
	 * @param message
	 * @param details
	 */
	public ResourceNotDestroyedFault(String message, ResourceNotDestroyedFaultType details) {
		super(message);
		this.faultDetail=details;
	}

	public static ResourceNotDestroyedFault createFault(){
		return createFault("");
	}
	
	public static ResourceNotDestroyedFault createFault(String msg){
		ResourceNotDestroyedFaultType bft=ResourceNotDestroyedFaultType.Factory.newInstance();
		bft.setTimestamp(Calendar.getInstance());
		ResourceNotDestroyedFault f=new ResourceNotDestroyedFault("Resource was not destroyed: "+msg,bft);
		return f;
	}
	

}
