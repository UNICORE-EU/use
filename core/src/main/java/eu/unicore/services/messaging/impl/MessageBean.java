package eu.unicore.services.messaging.impl;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import de.fzj.unicore.persist.annotations.Column;
import de.fzj.unicore.persist.annotations.ID;
import de.fzj.unicore.persist.annotations.Table;
import de.fzj.unicore.persist.util.GSONConverter;
import de.fzj.unicore.persist.util.JSON;
import eu.unicore.services.messaging.Message;

@Table(name="MessageQueueStorage")
@JSON(customHandlers={MessageBean.Adapter.class})
public class MessageBean implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@ID
	String id;
	
	@Column(name="destination")
	private String destination;

	private MessageWrapper messageHolder;
	
	public String getDestination() {
		return destination;
	}

	public String getID(){
		return id;
	}
	
	public Message getMessage(){
		return messageHolder.getMessage();
	}
	
	public MessageBean(String id, String destination, Message message){
		this.id=id;
		this.destination=destination;
		this.messageHolder=new MessageWrapper(message);
	}
	
	public static class MessageWrapper {
		
		private String messageClass;
		
		private Message message;
		
		public MessageWrapper(Message m){
			this.message=m;
			this.messageClass=m.getClass().getName();
		}

		public String getMessageClass() {
			return messageClass;
		}

		public Message getMessage() {
			return message;
		}

	}

	public static class Adapter implements GSONConverter{

		@Override
		public Type getType() {
			return MessageWrapper.class;
		}

		@Override
		public Object[] getAdapters() {
			return new Object[]{adapter};
		}
		
		@Override
		public boolean isHierarchy(){
			return true;
		}
	}
	
	private static final MessageAdapter adapter=new MessageAdapter();
	
	public static class MessageAdapter implements JsonDeserializer<MessageWrapper>{
		@Override
		public MessageWrapper deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context)
						throws JsonParseException {
			String className = json.getAsJsonObject().get("messageClass").getAsString();
			try{
				Class<?>clazz = Class.forName(className);
				Message message =  context.deserialize(json.getAsJsonObject().get("message"),clazz);
				return new MessageWrapper(message);
			}catch(ClassNotFoundException cne){
				throw new JsonParseException("Unknown model class", cne);
			}
		}
	}

}
