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
 

package eu.unicore.services.ws.exampleservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;

import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;

/**
 *  Example WSRF factory interface for testing
 */

@WebService(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface IExampleFactory {

	@WebMethod(action="addTestResource",operationName="AddTestResource")
	@WebResult(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite",name="AddTestResourceResponse")
	AddTestResourceResponseDocument addTestResource(
			@WebParam(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite",name="AddTestResource")
			AddTestResourceDocument req) throws BaseFault;
	

	@WebMethod(action="forbiddenAddTestResource")
	@WebResult(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite",name="AddTestResourceResponse")
	AddTestResourceResponseDocument forbiddenAddTestResource(
			@WebParam(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite",name="AddTestResource")
			AddTestResourceDocument req) throws BaseFault;
	
}
