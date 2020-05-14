package eu.unicore.uas.pdp.request.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.unicore.security.XACMLAttribute;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta.XACMLAttributeCategory;

public abstract class XACMLProfileBase implements XACMLProfile {

	protected Map<String, XACMLAttributeMeta> attributes;
	protected Set<String> reservedAttrName;
	protected String siteUrl;
	

	public XACMLProfileBase(String siteUrl) {
		this.siteUrl = siteUrl;
		attributes = new HashMap<String, XACMLAttributeMeta>();
		reservedAttrName = new HashSet<String>();

		addAttributeDef(new XACMLAttributeMeta(
				XACMLAttribute.Name.XACML_SUBJECT_ID_ATTR.toString(),
				XACMLAttribute.Type.X500NAME.toString(), 
				XACMLAttributeCategory.Subject));

		addAttributeDef(new XACMLAttributeMeta(
				XACMLAttribute.Name.XACML_ACTION_ID_ATTR.toString(),
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Action));

	}

	@Override
	public boolean checkAttr(String attrName) {
		return !attributes.keySet().contains(attrName);
	}

	@Override
	public List<XACMLAttributeMeta> getByCategory(XACMLAttributeCategory c) {
		ArrayList<XACMLAttributeMeta> res = new ArrayList<XACMLAttributeMeta>();
		for (XACMLAttributeMeta a : attributes.values()) {
			if (a.getCategory().equals(c))
				res.add(a);

		}
		Collections.sort(res);
		return res;
	}

	public void initReservedAttr() {
		for (XACMLAttributeMeta a : attributes.values()) {
			reservedAttrName.add(a.getName());
		}
	}
	
	protected void addAttributeDef(XACMLAttributeMeta attributeMetadata) {
		XACMLAttributeMeta old = attributes.put(attributeMetadata.getName(), attributeMetadata);
		if (old != null)
			throw new RuntimeException("BUG! Adding attribute type twice " +
					"with the same name: " + attributeMetadata);
	}
	
	@Override
	public String getServiceUrl()
	{
		return siteUrl;
	}
}
