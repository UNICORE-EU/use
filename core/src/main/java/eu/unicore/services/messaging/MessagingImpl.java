package eu.unicore.services.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.BlockingArrayQueue;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
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

	private final Thread messagingWriteThread;
	
	final BlockingQueue<MessageBean> queue;
	
	public MessagingImpl(PersistenceProperties kernelCfg)throws Exception{
		this.persistenceProperties = kernelCfg;
		this.store = createStore();
		this.queue = new BlockingArrayQueue<>(8192);
		this.messagingWriteThread = new MessageWriteThread(queue,store);
		this.messagingWriteThread.start();
		this.providers = new HashMap<>();
	}

	protected Persist<MessageBean>createStore()throws PersistenceException{
		return PersistenceFactory.get(persistenceProperties).getPersist(MessageBean.class);
	} 
	
	public int getStoredMessages()throws Exception{
		return store.getRowCount();
	}

	public void registerProvider(IMessagingProvider provider, String id) {
		if(provider!=null && id!=null){
			if(providers.put(id,provider)==null){
				logger.info("Registered new provider <{}> as <{}>", provider.getClass().getName(), id);
			}
		}else {
			logger.warn("Null provider or id, continuing.");
		}
	}

	public boolean hasMessages(String destination){
		try{
			return store.getRowCount("destination", destination)>0;
		}catch(Exception ex){
			return false;
		}
	}

	/**
	 * get a pull point for reading internal messages 
	 * targeted at the given destination
	 * The messages will be removed from the store, so the application has to process them all,
	 * or call
	 */
	public PullPoint getPullPoint(final String destination) throws Exception {
		final List<Message>messages = new ArrayList<>();
		synchronized(store){
			for(String messageID: store.getIDs("destination",destination)){
				messages.add(store.read(messageID).getMessage());
				store.remove(messageID);
			}
		}

		return new PullPoint(){

			Iterator<Message> messageIterator = messages.iterator();

			public boolean hasNext() {
				return messageIterator.hasNext();
			}

			public Message next() {
				Message m = messageIterator.next();
				messageIterator.remove();
				return m;
			}

			public void dispose(){
				synchronized(store){
					while(hasNext()){
						Message next=next();
						try{
							store.write(new MessageBean(next.getMessageId(),destination, next));
						}catch(Exception pe){
							logger.warn("Could not write message.");
						}
					}
				}
			}
		};

	}

	public IMessagingChannel getChannel(final String name) throws Exception {

		IMessagingProvider provider = providers.get(name);

		if(provider!=null){
			return provider.getChannel();
		}

		//fallback to internal messaging
		return new IMessagingChannel(){
			public void publish(Message message) throws Exception{
				queue.offer(new MessageBean(message.getMessageId(),name,message));
			}
		};
	}

	public void cleanup() {
		try {
			store.removeAll();
		}catch(Exception e) {
			Log.logException("", e);
		}
	}

	public static class MessageWriteThread extends Thread {

		private final BlockingQueue<MessageBean> queue;

		private final Persist<MessageBean> store;

		public MessageWriteThread(BlockingQueue<MessageBean> queue, Persist<MessageBean> store) {
			this.setName("use-messaging-writer");
			this.queue = queue;
			this.store = store;
		}

		public void run() {
			while(true) {
				try{
					MessageBean msg = queue.poll(60, TimeUnit.SECONDS);
					if(msg!=null)store.write(msg);
				}catch(InterruptedException te) {
					// ignored
				}catch(Exception ex) {
					logger.error(ex);
				}
			}	
		}
	}

}
