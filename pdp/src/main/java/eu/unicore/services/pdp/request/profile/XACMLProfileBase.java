package eu.unicore.services.pdp.request.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.pdp.request.creator.XACMLAttributeMeta;
import eu.unicore.services.pdp.request.creator.XACMLAttributeMeta.XACMLAttributeCategory;

public abstract class XACMLProfileBase implements XACMLProfile {

	protected final Map<String, XACMLAttributeMeta> attributes = new HashMap<>();

	protected final String siteUrl;	

	public XACMLProfileBase(String siteUrl) {
		this.siteUrl = siteUrl;
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
		ArrayList<XACMLAttributeMeta> res = new ArrayList<>();
		for (XACMLAttributeMeta a : attributes.values()) {
			if (a.getCategory().equals(c))res.add(a);
		}
		Collections.sort(res);
		return res;
	}

	protected void addAttributeDef(XACMLAttributeMeta attributeMetadata) {
		XACMLAttributeMeta old = attributes.put(attributeMetadata.getName(), attributeMetadata);
		if (old != null)
			throw new RuntimeException("BUG! Adding attribute type twice with the same name: "+
						attributeMetadata);
	}

}
