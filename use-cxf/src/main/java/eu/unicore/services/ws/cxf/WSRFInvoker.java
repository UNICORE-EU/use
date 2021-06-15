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


package eu.unicore.services.ws.cxf;

import java.lang.reflect.Method;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.AbstractInvoker;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.codahale.metrics.Meter;

import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.security.SecurityManager;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.WSFrontEnd;
import eu.unicore.util.Log;

/**
 * an invoker for WSRF services
 * 
 * @author schuller
 */
public class WSRFInvoker extends AbstractInvoker {
	
	private static final Logger logger=Log.getLogger(Log.UNICORE,WSRFInvoker.class);

	/**
	 * property key for storing the service instance object in the message context 
	 */
	public static final String KEY=WSRFInvoker.class.getName()+"KEY";
	
	/**
	 * property key for storing the WSRF frontend instance in the message context 
	 */
	public static final String FRONTEND_KEY=WSRFInvoker.class.getName()+"WSRF";
	
	
	private static final String METHODKEY=WSRFInvoker.class.getName()+"METHOD";
	public static final String LOCKEDKEY=WSRFInvoker.class.getName()+"LOCKED";
	private static final String CLEANUPKEY=WSRFInvoker.class.getName()+"CLEANUP";
	
	private static Meter callFrequency; 
	
	private final Kernel kernel;
	
	public WSRFInvoker(Kernel kernel){
		this.kernel=kernel;
		setupMetrics();
	}
	
	private synchronized void setupMetrics(){
		if(callFrequency == null){
			callFrequency=new Meter();
			kernel.getMetricRegistry().register("use.wsrf.callFrequency",callFrequency);
		}
	}
	
	/**
	 * Creates and returns a service object
	 */
	@Override
	public Object getServiceObject(final Exchange context) {
		Resource resource=(Resource)context.get(KEY);
		//run activation code. this is run here, i.e. after a possible access control check
		resource.activate();
		WSFrontEnd frontend = (WSFrontEnd)context.get(FRONTEND_KEY);
		return frontend!=null ? frontend : resource;
	}

	@Override
	protected Object performInvocation(Exchange ctx, Object serviceObject, Method method, Object[]paramArray)
	throws Exception {
		ctx.put(METHODKEY, method.getName());
		try{
			boolean local=CXFUtils.isLocalCall(ctx);
			if(local){
				SecurityManager.setLocalCall();
			}
			return super.performInvocation(ctx, serviceObject, method, paramArray);
		}
		finally{
			cleanUp(ctx);
			if(!CXFUtils.isLocalCall(ctx)){
				synchronized(callFrequency){
					callFrequency.mark();
				}
			}
		}
	}

	/**
	 * cleanup after a WSRF call, i.e. persist/unlock the service object
	 */
	public void cleanUp(Exchange ctx){
		if(ctx.get(CLEANUPKEY)!=null){
			//already cleaned up
			return;
		}
		SecurityManager.clearLocalCall();
		boolean locked=Boolean.TRUE.equals(ctx.get(LOCKEDKEY));
		ThreadContext.clearAll();
		Resource serviceInstance=(Resource)ctx.get(KEY);
		AuthZAttributeStore.clear();
		if(serviceInstance!=null){
			try{
				if(locked){
					if(!serviceInstance.isDestroyed()){
						serviceInstance.getHome().persist(serviceInstance);
					}
					else {
						String serviceName = serviceInstance.getServiceName();
						kernel.getPersistenceManager().getPersist(serviceName).unlock(serviceInstance);
					}
				}
			}catch(Exception e){
				Log.logException("Could not persist service instance " +
						"{"+serviceInstance.getServiceName()+"}"+serviceInstance.getUniqueID(),e,logger);
			}
			ctx.put(LOCKEDKEY, Boolean.FALSE);
			ctx.put(CLEANUPKEY, Boolean.TRUE);
		}
	}
}