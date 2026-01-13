package eu.unicore.services.messaging;

import java.io.Serializable;

import eu.unicore.services.utils.Utilities;

/**
 * a message 
 * 
 * @author schuller
 */
public abstract class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String messageId;

	public Message(){
		messageId = Utilities.newUniqueID();
	}

	public String getMessageId() {
		return messageId;
	}

}