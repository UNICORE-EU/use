/** Copyright (c) 2008 Forschungszentrum Juelich GmbH 
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
 
package de.fzj.unicore.wsrflite.xmlbeans.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import eu.unicore.util.Log;

/**
 * Talks to multiple WSRF services in a round-robin or multicast fashion.<br>
 * 
 * TODO support for plain ws
 * 
 * @author schuller
 */
public class MultiWSRFClient<T extends BaseWSRFClient> {

	protected static final Logger logger=Log.getLogger(Log.CLIENT,MultiWSRFClient.class);
	
	/**
	 * Multicast mode: all services will receive messages
	 */
	public static final int MULTICAST=1;
	
	/**
	 * Roundrobin mode
	 */
	public static final int ROUNDROBIN=2;
	
	/**
	 * Roundrobin mode, and calls will be retried if they fail
	 */
	public static final int ROUNDROBIN_RETRY_ON_FAILURE=3;
	
	/**
	 * The first service is the primary one, the others act as backup
	 */
	public static final int PRIMARY_WITH_BACKUP=4;
	
	
	protected List<T> clients=new ArrayList<T>();
	
	private int mode=MULTICAST;
	
	private int maxRetries=10;
	
	//any errors from the last call?
	private boolean errorsOccurred;
	
	public void addClient(T client){
		clients.add(client);
	}
	
	public boolean removeClient(T client){
		return clients.remove(client);
	}
	
	public void setMode(int mode){
		this.mode=mode;
	}
	
	/**
	 * create a proxy for the given interface
	 * 
	 * @param <Target>
	 * @param target
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <Target> Target makeProxy(Class<Target> target)throws Exception {
		List<Target>proxies=new ArrayList<Target>();
		for(T client: clients){
			proxies.add(client.makeProxy(target));
		}
		InvocationHandler h=getInvocationHandler(proxies);
		Object proxy=Proxy.newProxyInstance(target.getClassLoader(), new Class[]{target}, h);
		return (Target)proxy;
	}
	
	protected <Target> InvocationHandler getInvocationHandler(final List<Target>targets){
		MultiInvocationHandler<Target> h=new MultiInvocationHandler<Target>(this);
		h.setMode(mode);
		h.setTargets(targets);
		return h;
	}
	
	
	protected int getMaxRetries(){
		return maxRetries;
	}
	
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}
	
	void setErrorsOccurred(boolean errors){
		this.errorsOccurred=errors;
	}
	
	public boolean getErrorsOccurred(){
		return errorsOccurred;
	}
	
}
