package eu.unicore.services.messaging;

/**
 * Provides access to the messaging system of USE
 * 
 * @author schuller
 */
public interface IMessaging {

	/**
	 * register a new messaging provider
	 * 
	 * @see IMessagingProvider
	 * @param provider
	 */
	public void registerProvider(IMessagingProvider provider, String id);
	
	/**
	 * gets or creates a messaging channel with the given name
	 * 
	 * @param name -  the name of the messaging queue
	 * @return a {@link IMessagingChannel}
	 */
	public IMessagingChannel getChannel(String name) throws MessagingException;
	
	/**
	 * get a pullpoint for reading messages targeted at the given destination
	 * 
	 * @param destination - the name of the messaging queue
	 * @return a {@link PullPoint}
	 * @throws MessagingException
	 */
	public PullPoint getPullPoint(String destination) throws MessagingException;
	
	/**
	 * checks if unread messages exist for the given destination
	 * 
	 * @param destination
	 * @return <code>true</code> if unread messages exist
	 */
	public boolean hasMessages(String destination)throws MessagingException;
	
}
