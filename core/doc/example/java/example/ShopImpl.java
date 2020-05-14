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
 

package example;

import org.example.CreateShoppingCartDocument;
import org.example.CreateShoppingCartResponseDocument;
import org.w3.x2005.x08.addressing.AttributedURIType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;

public class ShopImpl implements IShop{
	
	public CreateShoppingCartResponseDocument
	   createNewShoppingCart(CreateShoppingCartDocument in)throws BaseFault{
		try{
			CreateShoppingCartResponseDocument response=
				CreateShoppingCartResponseDocument.Factory.newInstance();
			response.addNewCreateShoppingCartResponse();
			
			//create the service wsa:To address
			String addr=Kernel.getKernel().getProperty(Kernel.WSRF_BASEURL)+
			"/cart?res="+makeNewInstance();
			
			EndpointReferenceType eprt=EndpointReferenceType.Factory.newInstance();
			AttributedURIType uri=eprt.addNewAddress();
			uri.setStringValue(addr);
			response.getCreateShoppingCartResponse().setEndpointReference(eprt);
			
			return response;
		}catch(Exception e){
			throw BaseFault.createFault(e.getMessage());
		}
	}

	/**
	 * create a new WSResource on the associated WSRF service
	 * @return
	 */
	protected String makeNewInstance() throws Exception {
		return Kernel.getKernel().getServiceHome("cart").createWSRFServiceInstance(null);
	}

}
