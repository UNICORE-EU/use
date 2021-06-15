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
 

package eu.unicore.services.ws;

import java.util.Calendar;

import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.oasisOpen.docs.wsrf.bf2.BaseFaultDocument;
import org.oasisOpen.docs.wsrf.bf2.BaseFaultType;

import eu.unicore.util.Log;

/**
 * represents a WSRF BaseFault.<br>
 * 
 * @author schuller
 */
@WebFault(name="BaseFault", targetNamespace="http://docs.oasis-open.org/wsrf/bf-2")
public class BaseFault extends Exception implements
	FaultOutInterceptor.FaultInfoException{
	
	protected static final long serialVersionUID=1L;
	
	private BaseFaultType faultDetail;
	
	public BaseFault(String message, Throwable cause, BaseFaultType details){
		super(message,cause);
		this.faultDetail=details;
	}

	public BaseFault(String message, BaseFaultType details){
		super(message);
		this.faultDetail=details;
	}

	public BaseFault(String message){
		super(message);
	}
	
	public BaseFaultType getFaultInfo() {
		return faultDetail;
	}
	
	public static QName getFaultName() {
		return BaseFaultDocument.type.getDocumentElementName();
	}
	
	/**
	 * helper for creating a BaseFault including the mandatory timestamp
	 * @param message - fault message
	 */
	public static BaseFault createFault(String message){
		return createFault(message, null, false);
	}

	/**
	 * helper for creating a BaseFault including the mandatory timestamp
	 * @param message - the fault message
	 * @param cause - the underlying exception (which may be null)
	 */
	public static BaseFault createFault(String message, Throwable cause){
		return createFault(message, cause, true);
	}

	/** 
	 * Helper for creating a BaseFault including the mandatory timestamp. 
	 * This version allows for controlling whether a detailed message should be created
	 * using the message contained in the cause of the fault. 
	 * @param message the fault message
	 * @param cause the cause
	 * @param addDetails whether to use cause parameter to get additional information  
	 */
	public static BaseFault createFault(String message, Throwable cause, boolean addDetails) {
		BaseFaultType bft=BaseFaultType.Factory.newInstance();
		bft.setTimestamp(Calendar.getInstance());
		StringBuilder finalMsg = new StringBuilder();
		finalMsg.append(message);
		if (addDetails && cause!=null) {
			finalMsg.append(" Reason: ").append(Log.getDetailMessage(cause));
		} 
		bft.addNewDescription().setStringValue(finalMsg.toString());
		return cause == null ? new BaseFault(finalMsg.toString(), bft) :
			new BaseFault(finalMsg.toString(), cause, bft);
	}

}
