package eu.unicore.services.messaging;

/**
 * used to notify a "parent" resource that a "child" resource 
 * has been added
 * 
 * @author schuller
 */
public class ResourceAddedMessage extends Message {

	private static final long serialVersionUID = 1L;

	private final String addedResource;
	
	private final String serviceName;
	
	public ResourceAddedMessage(String serviceName, String addedResource) {
		super("added:"+addedResource);
		this.addedResource=addedResource;
		this.serviceName=serviceName;
	}

	public String getAddedResource() {
		return addedResource;
	}

	public String getServiceName() {
		return serviceName;
	}

}