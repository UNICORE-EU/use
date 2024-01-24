package eu.unicore.services.messaging;

/**
 * messages for a specific destination can be pulled off a queue using
 * this interface
 * 
 * @author schuller
 */
public interface PullPoint {

	/**
	 * returns true if there is still a message to be read
	 */
	public boolean hasNext();
	
	/**
	 * read the next message and remove if from the queue
	 */
	public Message next();

	/**
	 * abort reading the messages, writing back any unread messages to the queue
	 */
	public void dispose();
	
}
