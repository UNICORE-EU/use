package eu.unicore.services.pdp.request.creator;

import java.util.ArrayList;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.security.Client;
import eu.unicore.services.pdp.request.creator.XACMLAttributeMeta.XACMLAttributeCategory;
import eu.unicore.services.pdp.request.profile.XACMLProfile;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.ResourceDescriptor;
import xmlbeans.oasis.xacml.x2.x0.context.ActionType;
import xmlbeans.oasis.xacml.x2.x0.context.AttributeType;
import xmlbeans.oasis.xacml.x2.x0.context.AttributeValueType;
import xmlbeans.oasis.xacml.x2.x0.context.EnvironmentType;
import xmlbeans.oasis.xacml.x2.x0.context.RequestType;
import xmlbeans.oasis.xacml.x2.x0.context.ResourceType;
import xmlbeans.oasis.xacml.x2.x0.context.SubjectType;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLAuthzDecisionQueryDocument;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLAuthzDecisionQueryType;

public class XmlbeansXacml2RequestCreator extends RequestCreatorBase {

	public XmlbeansXacml2RequestCreator(XACMLProfile p) {
		super(p);
	}

	public XACMLAuthzDecisionQueryDocument createRequest(Client c,
			ActionDescriptor action, ResourceDescriptor des) {
		validateClient(c);

		AuhzDecisionRequest req = new AuhzDecisionRequest(new NameID(profile.getServiceUrl(),
				SAMLConstants.NFORMAT_ENTITY));

		XACMLAuthzDecisionQueryDocument reqDoc = req.getXMLBeanDoc();
		XACMLAuthzDecisionQueryType xreq = reqDoc.getXACMLAuthzDecisionQuery();
		RequestType xacmlRequest = xreq.addNewRequest();

		EnvironmentType env = xacmlRequest.addNewEnvironment();

		env.setAttributeArray(getAttrArray(XACMLAttributeCategory.Environment,
				c, action, des));

		ActionType xAction = xacmlRequest.addNewAction();

		xAction.setAttributeArray(getAttrArray(XACMLAttributeCategory.Action,
				c, action, des));

		ResourceType resource = xacmlRequest.addNewResource();
		resource.setAttributeArray(getAttrArray(
				XACMLAttributeCategory.Resource, c, action, des));

		SubjectType subject = xacmlRequest.addNewSubject();
		subject.setAttributeArray(getAttrArray(XACMLAttributeCategory.Subject,
				c, action, des));

		return reqDoc;

	}

	public AttributeType[] getAttrArray(XACMLAttributeCategory cat, Client c,
			ActionDescriptor action, ResourceDescriptor des) {
		ArrayList<AttributeType> ar = new ArrayList<AttributeType>();
		for (XACMLAttributeMeta a : profile.getByCategory(cat)) {
			for (String v : profile.getValue(a, c, action, des)) {
				AttributeType aa = AttributeType.Factory.newInstance();
				setXACMLAttribute(aa, a.getName(), a.getType().toString(), v);
				ar.add(aa);
			}
		}
		AttributeType[] res = (AttributeType[]) ar.toArray(
				new AttributeType[ar.size()]);
		return res;

	}

	public void setXACMLAttribute(
			xmlbeans.oasis.xacml.x2.x0.context.AttributeType a, String id,
			String type, String value) {
		a.setAttributeId(id);
		a.setDataType(type);
		AttributeValueType av = a.addNewAttributeValue();
		XmlObject val = XmlObject.Factory.newInstance();
		XmlCursor valCursor = val.newCursor();
		valCursor.toNextToken();
		valCursor.insertChars(value);
		valCursor.dispose();
		av.set(val);
	}

}
