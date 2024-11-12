package eu.unicore.services.messaging;

public interface IMessagingChannel {

	public void publish(Message message) throws Exception;

	// only use for testing - publishing messages should be async
	public default void flush() throws Exception {};
}
