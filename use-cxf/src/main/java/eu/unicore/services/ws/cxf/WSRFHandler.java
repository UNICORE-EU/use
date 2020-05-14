/*********************************************************************************
 * Copyright (c) 2011-2012 Forschungszentrum Juelich GmbH 
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

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.Service;
import de.fzj.unicore.wsrflite.admin.ResourceAvailability;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnavailableException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.messaging.MessagingException;
import de.fzj.unicore.wsrflite.persistence.PersistenceSettings;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.ws.impl.XmlBeansFaultConverter;
import eu.unicore.util.Log;

/**
 * for WSRF services, this handler resolves the required WSRF instance
 * 
 * @author schuller
 */
public class WSRFHandler extends AbstractPhaseInterceptor<Message> {

	private static final Logger logger=Log.getLogger(Log.WSRFLITE,WSRFHandler.class);
	
	private final Kernel kernel;
	
	public WSRFHandler(Kernel kernel) {
		super(org.apache.cxf.phase.Phase.PRE_INVOKE);
		getAfter().add(MAPCodec.class.getName());
		this.kernel=kernel;
	}

	public void handleMessage(Message message){
		String serviceName=CXFKernel.getSimpleServiceName(message);
		Service service=kernel.getService(serviceName);
		if(service==null){
			throw new Fault(new ResourceUnknownException("The requested service '"+serviceName+"' does not exist."));
		}
		setupServiceObject(message);
	}
	
	/**
	 * retrieve the correct service instance id, based on the incoming message
	 * 
	 * @param inMessage - incoming message
	 * @return the service instance ID
	 */
	public String extractServiceReference(Message inMessage)throws ResourceUnknownException {

		String id=null;
		SoapMessage message=(SoapMessage)inMessage;

		try{
			AddressingProperties add=ContextUtils.retrieveMAPs(message, false, false);
			if(add!=null && add.getTo()!=null){
				//first check wsa:To header
				String url=add.getTo().getValue();
				if(url!=null){
					//assume format "blah?res=id"
					try {
						String[] ids=url.split("=");
						if(ids.length>1){
							id=ids[1];
						}
					} catch (RuntimeException e1) {}
				}

				if(id==null){
					//check reference parameters
					ReferenceParametersType ref=add.getToEndpointReference().getReferenceParameters();
					if(ref!=null){
						for(Object o: ref.getAny()){
							//TODO
							System.out.println(o);
						}
					}
				}
			}

			if(id==null){
				//fallback: try to use id from HTTP query string
				HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
				String query=request.getQueryString();
				if(query!=null){
					//assume format "res=id"
					try {
						String[] ids=query.split("=");
						id=ids[1];
					} catch (RuntimeException e1) {}
				}
			}
		}
		catch(RuntimeException e){ 
			Log.logException("Could not retrieve service reference from the incoming message.", e, logger);
		}

		if(id!=null){
			return id;
		}
		else throw new ResourceUnknownException("Could not retrieve service reference from the incoming message.");
	}

	
	/**
	 * resolves, initialises and returns the requested service object
	 */
	public Object setupServiceObject(final Message message){
		String serviceName=CXFKernel.getSimpleServiceName(message);
		Home homeObject=kernel.getHome(serviceName);
		
		if(homeObject==null){
			throw ResourceUnknownFault.createFault("The requested service '"+serviceName+"' does not exist.");
		}
		
		// this is the object that the WS call will be executed upon (i..e the ws frontend)
		Object serviceObject = null;
		
		// the actual resource
		Resource resource = null;
		
		String id=null;
		try{
			id=extractServiceReference(message);
			if(ResourceAvailability.isUnavailable(id)){
				throw new ResourceUnavailableException("Resource <"+id+"> has been temporarily disabled by an administrator.");
			}
			
			Method method=CXFUtils.getMethod(message);
			
			boolean hasMessages=false;
			try{
				hasMessages=kernel.getMessaging().hasMessages(id);
			}catch(MessagingException e){
				Log.logException("Error getting messages for "+id,e,logger);
			}
			resource = homeObject.get(id);
			serviceObject = CXFKernel.createFrontEnd(resource);
			PersistenceSettings ps=kernel.getPersistenceManager().getPersistenceSettings(serviceObject.getClass());
			// we must lock if the service method requires it, OR if we have internal updates 
			boolean needLock = !ps.isConcurrentMethod(method) || hasMessages;
			if(needLock){
				// upgrade to write lock!
				message.getExchange().put(WSRFInvoker.LOCKEDKEY, Boolean.TRUE);
				resource = homeObject.getForUpdate(id);
				serviceObject = CXFKernel.createFrontEnd(resource);
			}
		}catch(Exception pe){
			Exception converted = pe;
			try{
				converted = XmlBeansFaultConverter.getInstance().convert(pe);
			}catch(Exception ex){}
			if(converted instanceof RuntimeException){
				throw (RuntimeException)converted;
			}
			else throw new Fault(converted);
		}
		message.getExchange().put(WSRFInvoker.KEY,resource);
		message.getExchange().put(WSRFInvoker.FRONTEND_KEY,serviceObject);
		return serviceObject;
	}
	
	private final AtomicInteger faults=new AtomicInteger(0);
	
	@Override
	public void handleFault(Message message){
		faults.incrementAndGet();
		Exchange exch=message.getExchange();
		WSRFInvoker invoker=(WSRFInvoker)exch.getService().getInvoker();
		invoker.cleanUp(exch);
	}

	public int getFaults(){
		return faults.get();
	}

	public void clearFaults(){
		faults.set(0);
	}

}
