package eu.unicore.services.pdp.request.creator;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.herasaf.xacml.core.context.impl.ActionType;
import org.herasaf.xacml.core.context.impl.AttributeType;
import org.herasaf.xacml.core.context.impl.AttributeValueType;
import org.herasaf.xacml.core.context.impl.EnvironmentType;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.context.impl.ResourceType;
import org.herasaf.xacml.core.context.impl.SubjectType;
import org.herasaf.xacml.core.converter.DataTypeJAXBTypeAdapter;

import eu.unicore.security.Client;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.pdp.request.creator.XACMLAttributeMeta.XACMLAttributeCategory;
import eu.unicore.services.pdp.request.profile.XACMLProfile;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.Log;

public class HerasafXacml2RequestCreator extends RequestCreatorBase {

	private static final Logger log = Log.getLogger(Log.SECURITY, HerasafXacml2RequestCreator.class);

	public HerasafXacml2RequestCreator(XACMLProfile p) {
		super(p);
	}

	public RequestType createRequest(Client c, ActionDescriptor action, ResourceDescriptor des) {
		validateClient(c);
		RequestType req = new RequestType();
		List<SubjectType> subjects = req.getSubjects();
		SubjectType subject = new SubjectType();
		subjects.add(subject);
		List<AttributeType> subjectAttrs = subject.getAttributes();
		setAttrList(subjectAttrs, XACMLAttributeCategory.Subject, c, action, des);
		String warn = addHeresafAttributesFromAIPs(c, subjectAttrs);
		if (warn.length() != 0)log.warn(warn);
		List<ResourceType> resources = req.getResources();
		ResourceType resource = new ResourceType();
		resources.add(resource);
		List<AttributeType> resourceAttrs = resource.getAttributes();
		setAttrList(resourceAttrs, XACMLAttributeCategory.Resource, c, action, des);
		ActionType xacmlAction = new ActionType();
		List<AttributeType> actionAttrs = xacmlAction.getAttributes();
		setAttrList(actionAttrs, XACMLAttributeCategory.Action, c, action, des);
		req.setAction(xacmlAction);
		EnvironmentType env = new EnvironmentType();
		List<AttributeType> envAttrs = env.getAttributes();
		setAttrList(envAttrs, XACMLAttributeCategory.Environment, c, action, des);
		req.setEnvironment(env);
		return req;
	}

	public void setAttrList(List<AttributeType> attrs, XACMLAttributeCategory cat, Client c,
			ActionDescriptor action, ResourceDescriptor des) {
		for (XACMLAttributeMeta a : profile.getByCategory(cat)) {
			for (String v : profile.getValue(a, c, action, des)) {
				attrs.add(getHerasafAttribute(a, v));
			}
		}
	}

	public AttributeType getHerasafAttribute(XACMLAttributeMeta xacmlAttribute, String value) {
		AttributeType attribute = new AttributeType();
		DataTypeJAXBTypeAdapter converter = new DataTypeJAXBTypeAdapter();
		attribute.setAttributeId(xacmlAttribute.getName());
		attribute.setDataType(converter.unmarshal(xacmlAttribute.getType()));
		AttributeValueType avalue = new AttributeValueType();
		avalue.getContent().add(value);
		attribute.getAttributeValues().add(avalue);
		return attribute;
	}

	public String addHeresafAttributesFromAIPs(Client client, List<AttributeType> subjectAttrs) {
		StringBuilder warn = new StringBuilder();
		for (XACMLAttribute attr : client.getSubjectAttributes().getXacmlAttributes()) {
			if (!profile.checkAttr(attr.getName())) {
				warn.append("Among clients GENERIC XACML attributes retrieved from the "
						+ "configured attribute sources, the special attribute "
						+ attr.getName() + " was found. Ignoring it.\n");
				continue;
			}
			AttributeType herasfAttribute = getHerasafAttribute(
					new XACMLAttributeMeta(attr.getName(), attr.getType()
							.toString(), XACMLAttributeCategory.Subject), attr
							.getValue());
			subjectAttrs.add(herasfAttribute);
		}
		return warn.toString();
	}
}
