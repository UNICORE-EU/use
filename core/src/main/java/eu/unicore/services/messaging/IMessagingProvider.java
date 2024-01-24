package eu.unicore.services.messaging;

/**
 * Messaging provider interface. This has just one method which returns
 * a channel for writing messages to
 * 
 * @author schuller
 */
public interface IMessagingProvider {

	public IMessagingChannel getChannel();
	
}
