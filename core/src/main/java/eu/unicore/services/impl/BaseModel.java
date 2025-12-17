package eu.unicore.services.impl;

import eu.unicore.services.ExtendedResourceStatus;
import eu.unicore.services.ExtendedResourceStatus.ResourceStatus;

public class BaseModel extends SecuredResourceModel {

	private static final long serialVersionUID = 1L;

	private String resourceStatusDetails = "N/A";

	private ExtendedResourceStatus.ResourceStatus resourceStatus = ResourceStatus.UNDEFINED;

	public String getResourceStatusDetails() {
		return resourceStatusDetails;
	}

	public void setResourceStatusDetails(String resourceStatusDetails) {
		this.resourceStatusDetails = resourceStatusDetails;
	}

	public ExtendedResourceStatus.ResourceStatus getResourceStatus() {
		return resourceStatus;
	}

	public void setResourceStatus(ExtendedResourceStatus.ResourceStatus resourceStatus) {
		this.resourceStatus = resourceStatus;
	}

}