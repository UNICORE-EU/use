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


package eu.unicore.services.ws.impl;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.WSRFConstants;
import eu.unicore.services.impl.DefaultHome;

/**
 * 
 * XMLBeans bound implementation of the Home class. 
 * It is responsible for creating new WS-Resources.<br>
 * 
 * When subclassing this, usually it is enough to override 
 * the doCreateInstance() method.<br>
 * 
 * Note: the actual WS-Resource to be used is selected either <br>
 * a) from the query part of the WS-Addressing To: field <br>
 * b) from reference parameters given in the SOAP header.<br>
 * To use the second method, make sure to override the  
 * getReferenceParameterQNames() method, and return the QNames of 
 * the XML element you want to use as reference parameter.
 * <br>
 * 
 * As a second task, the home class converts some exceptions related to lifecycle
 * to the corresponding faults
 * 
 * @author schuller
 */
public class WSResourceHomeImpl extends DefaultHome {

	private List<QName>acceptedRefParams=Arrays.asList(new QName[]{
			new QName("http://unicore.sourceforge.net","resId"),
			WSRFConstants.U6_RESOURCE_ID,
			//old GPE clients still use this
			new QName("http://com.fujitsu.arcon.addressing","ResourceDisambiguator"),
	});

	/**
	 * if you want to use referenceParameters for selecting resources,
	 * return a list of accepted qnames here
	 */
	protected List<QName> getReferenceParameterQNames(){
		return acceptedRefParams;
	}

	public WSResourceHomeImpl(){
		super();
	}

	public WSResourceHomeImpl(Kernel kernel){
		super();
		setKernel(kernel);
	}
	
	@Override
	protected Resource doCreateInstance()throws Exception{
		Object o = Class.forName("eu.unicore.services.ws.MockResource").getConstructor().newInstance();
		return (Resource)o; 
	}

}
