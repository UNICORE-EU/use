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


package eu.unicore.services.ws.utils;

import javax.xml.namespace.QName;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.ws.WSUtilities;

/**
 * Server side tools and utilities, mainly for working with endpoint references, soap headers, and WSRF
 * related XML types. EPRs created will include the server identity.
 * 
 * <em>On the client side use {@link WSUtilities}.</em>
 * 
 * @author schuller
 * @author demuth
 */
public class WSServerUtilities extends WSUtilities {

	private WSServerUtilities() {
	}


	/**
	 * create a new EPR that contains a metadata element 
	 * specifying the server name 
	 * @return {@link EndpointReferenceType}
	 */
	public static EndpointReferenceType newEPR(IContainerSecurityConfiguration secCfg){
		return newEPR(true, secCfg);
	}

	public static EndpointReferenceType newEPR(boolean addServerID, IContainerSecurityConfiguration secCfg){
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		if(addServerID && secCfg.isSslEnabled()){
			try{
				String serverDN = secCfg.getCredential().getCertificate().getSubjectX500Principal().getName();
				addServerIdentity(epr, serverDN);
			}catch(Exception e){}
		}
		return epr;
	}

	/**
	 * builds an wsa Address for the given service + resourceID
	 * 
	 * @param serviceName
	 * @param resourceID
	 * @param cfg
	 */
	public static String makeAddress(String serviceName, String resourceID, ContainerProperties cfg){
		return makeServiceAddress(makeAddress(serviceName, cfg), resourceID);
	}

	/**
	 * builds an wsa Address for the given plain web service 
	 * 
	 * @param serviceName
	 * @param cfg
	 */
	public static String makeAddress(String serviceName, ContainerProperties cfg){
		return cfg.getBaseUrl()+"/"+serviceName;
	}

	/**
	 * create a new epr for addressing the specified ws resource
	 * @param serviceName
	 * @param resourceID
	 * @param kernel
	 */
	public static EndpointReferenceType makeEPR(String serviceName,String resourceID, Kernel kernel){
		EndpointReferenceType epr=newEPR(kernel.getContainerSecurityConfiguration());
		epr.addNewAddress().setStringValue(makeAddress(serviceName,resourceID, kernel.getContainerProperties()));
		return epr;
	}

	/**
	 * Return the epr of the specified service, created
	 * by appending the given service name to the server's base URL. 
	 * This EPR will also contain the server identity, if available.
	 * @param serviceName
	 * @param kernel
	 */
	public static EndpointReferenceType makeEPR(String serviceName, Kernel kernel){
		return makeEPR(serviceName,true, kernel);
	}

	public static EndpointReferenceType makeEPR(String serviceName, boolean addServerID, Kernel kernel){
		EndpointReferenceType epr=newEPR(addServerID, kernel.getContainerSecurityConfiguration());
		epr.addNewAddress().setStringValue(makeAddress(serviceName, kernel.getContainerProperties()));
		return epr;
	}

	/**
	 * create an WSRF endpoint reference, adding a UNICORE reference parameter and the port type (interface name metadata)
	 * 
	 * @param serviceName - the service name
	 * @param resourceID - the wsrf id
	 * @param portType - the port type
	 * @param kernel
	 * @return EPR 
	 */
	public static EndpointReferenceType makeEPR(String serviceName, String resourceID, QName portType, Kernel kernel){
		EndpointReferenceType epr=makeEPR(serviceName, resourceID, kernel);
		addUGSRefparamToEpr(epr, resourceID);
		addPortType(epr, portType);
		return epr;
	}	

	/**
	 * create an WSRF endpoint reference, adding a UNICORE reference parameter and the port type (interface name metadata)
	 * 
	 * @param serviceName - the service name
	 * @param resourceID - the wsrf id
	 * @param portType - the port type
	 * @param addServerID - whether to add the server DN
	 * @param kernel
	 * @return EPR 
	 */
	public static EndpointReferenceType makeEPR(String serviceName, String resourceID, QName portType, boolean addServerID, Kernel kernel){
		EndpointReferenceType epr=newEPR(addServerID, kernel.getContainerSecurityConfiguration());
		epr.addNewAddress().setStringValue(makeAddress(serviceName, resourceID, kernel.getContainerProperties()));
		addUGSRefparamToEpr(epr, resourceID);
		addPortType(epr, portType);
		return epr;
	}
	
	/**
	 * create an endpoint reference for a plain web service, adding the port type (interface name metadata)
	 * 
	 * @param serviceName - the service name
	 * @param portType - the port type
	 * @param kernel
	 * @return EPR 
	 */
	public static EndpointReferenceType makeEPR(String serviceName, QName portType, Kernel kernel){
		EndpointReferenceType epr=makeEPR(serviceName, kernel);
		addPortType(epr, portType);
		return epr;
	}
}
