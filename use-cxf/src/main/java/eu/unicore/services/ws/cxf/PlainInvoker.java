/*********************************************************************************
 * Copyright (c) 2012 Forschungszentrum Juelich GmbH 
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


package eu.unicore.services.ws.cxf;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.AbstractInvoker;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.KernelInjectable;

/**
 * an CXF Invoker for plain (non-WSRF) web services
 * 
 * @author schuller
 */
public class PlainInvoker extends AbstractInvoker {

	private final Kernel kernel;

	private Object serviceObject=null;

	public static final String SERVICE_IMPL_CLASS="PlainInvoker.serviceImplClass";
	
	public PlainInvoker(Kernel kernel){
		this.kernel=kernel;
	}

	/**
	 * Creates and returns the service object
	 */
	@Override
	public synchronized Object getServiceObject(final Exchange context){
		try{
			if(serviceObject==null){
				Service s=context.getService();
				Class<?>clazz=(Class<?>)s.get(SERVICE_IMPL_CLASS);
				serviceObject=clazz.newInstance();
				if(serviceObject instanceof KernelInjectable){
					((KernelInjectable)serviceObject).setKernel(kernel);
				}	
			}
			return serviceObject;
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

}