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
 

package eu.unicore.services.ws.exampleservice;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.KernelInjectable;
import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * the implementation of the example factory
 */
public class ExampleFactoryImpl implements IExampleFactory, KernelInjectable {

	private Kernel kernel;
	
	@Override
	public void setKernel(Kernel kernel) {
		this.kernel=kernel;
	}

	public ExampleFactoryImpl(){
		System.out.println("ExampleFactoryImpl created");
	}

	public AddTestResourceResponseDocument addTestResource(AddTestResourceDocument req) throws BaseFault{
		try {
			AddTestResourceResponseDocument response=
				AddTestResourceResponseDocument.Factory.newInstance();
			response.addNewAddTestResourceResponse();
			String id=makeNewInstance();
			EndpointReferenceType eprt=WSServerUtilities.makeEPR("service", id, kernel);
			response.getAddTestResourceResponse().setEndpointReference(eprt);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			throw BaseFault.createFault(e.getMessage());
		}
	}

	
	/**
	 * this method exists for testing access control
	 */
	@Override
	public AddTestResourceResponseDocument forbiddenAddTestResource(
			AddTestResourceDocument req) throws BaseFault {
		return addTestResource(req);
	}



	/**
	 * create a new WSResource on the associated WSRF service
	 */
	protected String makeNewInstance() throws Exception {
		return kernel.getHome("service").createResource(new InitParameters());
	}
}
