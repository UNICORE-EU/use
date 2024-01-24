package eu.unicore.services.messaging;

import java.io.Serializable;

import eu.unicore.services.utils.Utilities;

/**
 * a message 
 * 
 * @author schuller
 */
public class Message implements Serializable{

	private static final long serialVersionUID = 1L;

	private String messageId;
	
	private String body;
	
	public Message(){
		this(null);
	}
	
	/**
	 * construct a new message
	 *
	 * @param body
	 */
	public Message(String body){
		messageId=Utilities.newUniqueID();
		this.body=body;
	}
	
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
}
