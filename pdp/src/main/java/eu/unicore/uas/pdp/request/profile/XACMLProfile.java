package eu.unicore.uas.pdp.request.profile;

import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.services.pdp.ActionDescriptor;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta;



public interface XACMLProfile {	
	
	public List<XACMLAttributeMeta> getByCategory(XACMLAttributeMeta.XACMLAttributeCategory c);	
	public boolean checkAttr(String attrName);
	public List<String> getValue(XACMLAttributeMeta attribute, Client c,
		ActionDescriptor action, ResourceDescriptor des);
	public String getServiceUrl();
}
