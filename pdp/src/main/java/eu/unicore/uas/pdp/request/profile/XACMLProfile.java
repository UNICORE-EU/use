package eu.unicore.uas.pdp.request.profile;

import java.util.List;

import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;

import eu.unicore.security.Client;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta;



public interface XACMLProfile {	
	
	public List<XACMLAttributeMeta> getByCategory(XACMLAttributeMeta.XACMLAttributeCategory c);	
	public boolean checkAttr(String attrName);
	public List<String> getValue(XACMLAttributeMeta attribute, Client c,
		ActionDescriptor action, ResourceDescriptor des);
	public String getServiceUrl();
}
