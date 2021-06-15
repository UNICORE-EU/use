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

import javax.xml.namespace.QName;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.Resource;

/**
 * Render a service address<br> 
 * 
 * @author schuller
 */
public class FixedAddressRenderer extends AddressRenderer{

	private final String serviceSpec;
	
	/**
	 * @param docElementName - the QName of the wrapper element, which must be of type {@link EndpointReferenceType}
	 * @param serviceSpec - the fixed service name (plus optional resource id)
	 * @param addServerIdentity - whether to add the server DN in the generated XML
	 * @throws Exception 
	 */
	public FixedAddressRenderer(Resource parent, QName docElementName, String serviceSpec, boolean addServerIdentity){
		super(parent,docElementName,addServerIdentity);
		this.serviceSpec=serviceSpec;
	}
	
	
	/**
	 * override to provide the service name plus the optional resource id
	 * @return service detail. If <code>null</code> nothing will be rendered
	 */
	protected final String getServiceSpec(){
		return serviceSpec;
	}
	
}
