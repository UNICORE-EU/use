package eu.unicore.services.messaging;


/**
 * used to notify a "parent" resource that a "child" resource 
 * has been destroyed
 * 
 * @author schuller
 */
public class ResourceDeletedMessage extends Message {

	private static final long serialVersionUID = 1L;

	private String deletedResource;
	
	private String serviceName;
	
	public ResourceDeletedMessage(String serviceName, String deletedResource) {
		super(deletedResource);
		this.deletedResource=deletedResource;
		this.serviceName=serviceName;
	}

	public String getDeletedResource() {
		return deletedResource;
	}

	public void setDeletedResource(String deletedResource) {
		this.deletedResource = deletedResource;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public ResourceDeletedMessage(String body) {
		super(body);
	}

}
