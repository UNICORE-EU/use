package eu.unicore.services.security.util;

/**
 * Descriptor for a resource
 * 
 * @author schuller
 */
public class ResourceDescriptor {

	// e.g. "JobManagement"
	final String serviceName;
	
	// e.g. "default_registry"
	final String resourceID;
	
	//the DN (i.e. X500Principal.getName()) of the resource owner
	final String owner;
	
	// holds the result of any ACL check that was performed
	// if this is true, the XACML policy will allow the request  
	private boolean aclCheckOK = false;
	
	public ResourceDescriptor(String serviceName, String resourceID, String owner){
		this.serviceName=serviceName;
		this.resourceID=resourceID;
		this.owner=owner;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(serviceName);
		if(resourceID!=null)sb.append('[').append(resourceID).append(']');
		if(owner!=null)sb.append("[owner: ").append(owner).append(']');
		return sb.toString();
	}
	
	public String getOwner() {
		return owner;
	}

	public String getResourceID() {
		return resourceID;
	}

	public String getServiceName() {
		return serviceName;
	}

	public boolean isAclCheckOK() {
		return aclCheckOK;
	}

	public void setAclCheckOK(boolean aclCheckOK) {
		this.aclCheckOK = aclCheckOK;
	}
	
}
