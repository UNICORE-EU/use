package eu.unicore.services.messaging;

public interface IMessagingChannel {

	public void publish(Message message) throws Exception;

}
