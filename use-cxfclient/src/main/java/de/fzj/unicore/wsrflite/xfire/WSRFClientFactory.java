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

package de.fzj.unicore.wsrflite.xfire;

import java.util.List;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.security.wsutil.client.UnicoreWSClientFactory;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Extends {@link UnicoreWSClientFactory} 
 * with support for creation of WS-Addressing aware WSRF client proxies.
 * 
 * @author schuller
 * @author golbi
 */
public class WSRFClientFactory extends UnicoreWSClientFactory {

	public WSRFClientFactory() {
		super();
	}
	
	public WSRFClientFactory(IClientConfiguration sec){
		super(sec);
	}

	public IClientConfiguration getSecurityConfiguration(){
		return security;
	}

	/**
	 * Create a proxy for the service at the given EPR
	 * 
	 * @see de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient
	 * 
	 * @param iFace Interface class
	 * @param url The url to contact
	 * @param wsaEPR The EPR of the Service
	 */
	public synchronized <T> T createProxy(Class<T> iFace, String url, 
			EndpointReferenceType wsaEPR) throws Exception {
		
		T proxy=super.createPlainWSProxy(iFace, url);
		
		Client wsClient = getWSClient(proxy);
		List<Interceptor<? extends Message>> out=wsClient.getOutInterceptors();
		
		MAPAggregator mapAggregator=new MAPAggregator();
		MAPCodec mapCodec=new MAPCodec();
		
		out.add(new WSAOutHandler(wsaEPR));
		out.add(mapAggregator);
		out.add(mapCodec);
		
		List<Interceptor<? extends Message>> outF=wsClient.getOutFaultInterceptors();
		outF.add(mapAggregator);
		outF.add(mapCodec);
		
		List<Interceptor<? extends Message>> in=wsClient.getInInterceptors();
		in.add(mapAggregator);
		in.add(mapCodec);
		
		List<Interceptor<? extends Message>> inF=wsClient.getInFaultInterceptors();
		inF.add(mapAggregator);
		inF.add(mapCodec);

		return proxy;
	}

}
