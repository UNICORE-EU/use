package eu.unicore.services.pdp.request.profile;

import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.services.pdp.request.creator.XACMLAttributeMeta;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.ResourceDescriptor;



public interface XACMLProfile {	
	
	public List<XACMLAttributeMeta> getByCategory(XACMLAttributeMeta.XACMLAttributeCategory c);	
	public boolean checkAttr(String attrName);
	public List<String> getValue(XACMLAttributeMeta attribute, Client c,
		ActionDescriptor action, ResourceDescriptor des);
	public String getServiceUrl();
}
