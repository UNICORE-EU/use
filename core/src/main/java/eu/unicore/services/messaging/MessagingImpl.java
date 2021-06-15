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


package eu.unicore.services.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.Persist;
import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.persist.PersistenceFactory;
import de.fzj.unicore.persist.PersistenceProperties;
import eu.unicore.services.messaging.impl.MessageBean;
import eu.unicore.util.Log;

/**
 * manages the internal messaging system
 * 
 * @author schuller
 */
public class MessagingImpl implements IMessaging{
	
	private static final Logger logger=Log.getLogger(Log.UNICORE,MessagingImpl.class);

	private final Persist<MessageBean> store;

	private final HashMap<String,IMessagingProvider>providers;

	private final PersistenceProperties persistenceProperties;

	public MessagingImpl(PersistenceProperties kernelCfg)throws MessagingException{
		this.persistenceProperties=kernelCfg;
		try{
			store=createStore();
		}catch(PersistenceException pe){
			throw new MessagingException(pe);
		}
		providers=new HashMap<String,IMessagingProvider>();
	}

	protected Persist<MessageBean>createStore()throws PersistenceException{
		Persist<MessageBean>res=PersistenceFactory.get(persistenceProperties).getPersist(MessageBean.class);
		return res;
	} 
	
	public int getStoredMessages()throws PersistenceException{
		return store.getRowCount();
	}

	public void registerProvider(IMessagingProvider provider, String id) {
		if(provider!=null && id!=null){
			if(providers.put(id,provider)==null){
				logger.info("Registered new provider <"+provider.getClass().getName()+"> as <"+id+">");
			}
		}else {
			logger.warn("Null provider or id, continuing.");
		}
	}

	public boolean hasMessages(String destination){
		try{
			return store.getRowCount("destination", destination)>0;
		}catch(PersistenceException ex){
			return false;
		}
	}

	/**
	 * get a pull point for reading internal messages 
	 * targeted at the given destination
	 * The messages will be removed from the store, so the application has to process them all,
	 * or call
	 */
	public PullPoint getPullPoint(final String destination) throws MessagingException {
		final List<Message>messages=new ArrayList<Message>();
		synchronized(store){
			try{
				for(String messageID: store.getIDs("destination",destination)){
					messages.add(store.read(messageID).getMessage());
					store.remove(messageID);
				}
			}catch(Exception ex){
				throw new MessagingException(ex);
			}
		}
		
		return new PullPoint(){

			Iterator<Message> messageIterator=messages.iterator();

			public boolean hasNext() {
				return messageIterator.hasNext();
			}

			public Message next() {
				Message m=messageIterator.next();
				messageIterator.remove();
				return m;
			}
			
			public void dispose(){
				synchronized(store){
					while(hasNext()){
						Message next=next();
						try{
							store.write(new MessageBean(next.getMessageId(),destination, next));
						}catch(PersistenceException pe){
							logger.warn("Could not write message.");
						}
					}
				}
			}
		};

	}
	
	public IMessagingChannel getChannel(final String name) throws MessagingException {
		IMessagingProvider provider=providers.get(name);
		if(provider!=null){
			return provider.getChannel();
		}
		
		//fallback to internal messaging
		return new IMessagingChannel(){
			public void publish(Message message) throws MessagingException{
				try{
					store.write(new MessageBean(message.getMessageId(),name,message));
				}catch(Exception ex){
					throw new MessagingException(ex);
				}
			}
		};

	}

	/**
	 * remove all content
	 * @throws PersistenceException
	 */
	public void cleanup()throws PersistenceException{
		store.removeAll();
	}


}
