package eu.unicore.uas.pdp.request.profile;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.wsrflite.security.SecurityManager;
import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta.XACMLAttributeCategory;

public class UnicoreInternalProfile extends XACMLProfileBase {
	public static final String ATTR_SUBJECT_CONSIGNOR_XACML_ID = "consignor";
	public static final String ATTR_ROLE_XACML_ID = "role";
	public static final String ATTR_RESOURCE_OWNER_XACML_ID = "owner";
	public static final String ATTR_WSR_XACML_ID = "urn:unicore:wsresource";
	public static final String ATTR_ACTION_TYPE_ID = "actionType";
	
	public static final String ATTR_USER_ACL_CHECK_RESULT_XACML_ID = "aclCheckPassed";
	
	public static final String VOLESS_USER_VALUE = "NOT_A_VO_BOUND_REQUEST";

	public UnicoreInternalProfile(String serviceUrl) {
		super(serviceUrl);
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_SUBJECT_CONSIGNOR_XACML_ID,
				XACMLAttribute.Type.X500NAME.toString(), 
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_ROLE_XACML_ID, 
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_USER_ACL_CHECK_RESULT_XACML_ID, 
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				XACMLAttribute.Name.XACML_RESOURCE_ID_ATTR.toString(),
				XACMLAttribute.Type.ANYURI.toString(), 
				XACMLAttributeCategory.Resource));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_RESOURCE_OWNER_XACML_ID,
				XACMLAttribute.Type.X500NAME.toString(), 
				XACMLAttributeCategory.Resource));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_WSR_XACML_ID, 
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Resource));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_ACTION_TYPE_ID,
				XACMLAttribute.Type.STRING.toString(),
				XACMLAttributeCategory.Action));
	}
	
	@Override
	public List<String> getValue(XACMLAttributeMeta attribute, Client c,
			ActionDescriptor action, ResourceDescriptor des) {
		List<String> res = new ArrayList<String>();
		String attrName = attribute.getName();
		
		if (attrName.equals(ATTR_SUBJECT_CONSIGNOR_XACML_ID)) {
			if(c.getSecurityTokens()!=null){
				String consignor = c.getSecurityTokens()
						.getConsignorName();
				if (consignor != null)
					res.add(X500NameUtils.getComparableForm(consignor));
			}
	
		} else if (attrName.equals(ATTR_ROLE_XACML_ID)) {
			String role = c.getRole().getName();
			if (role != null)
				res.add(c.getRole().getName());
			
		} else if (attrName.equals(XACMLAttribute.Name.XACML_RESOURCE_ID_ATTR.toString())) {
			String s = des.getServiceName();
			if (s != null)
				res.add(des.getServiceName());
			
		} else if (attrName.equals(ATTR_RESOURCE_OWNER_XACML_ID)) {
			String n = des.getOwner();
			if (n != null)
				res.add(X500NameUtils.getComparableForm(n));
		} else if (attrName.equals(ATTR_USER_ACL_CHECK_RESULT_XACML_ID)) {
				res.add(String.valueOf(des.isAclCheckOK()));
				
		} else if (attrName.equals(ATTR_WSR_XACML_ID)) {
			String r = des.getResourceID();
			if (r != null)
				res.add(r);
			
		} else if (attrName.equals(XACMLAttribute.Name.XACML_SUBJECT_ID_ATTR.toString())) {
			String name = c.getDistinguishedName();
			if (name != null)
				res.add(X500NameUtils.getComparableForm(name));
			
		} else if (attrName.equals(XACMLAttribute.Name.XACML_ACTION_ID_ATTR.toString())) {
			if (action != null && action.getAction() != null) {
				res.add(action.getAction());
			} else
				res.add(SecurityManager.UNKNOWN_ACTION);
		} else if (attrName.equals(ATTR_ACTION_TYPE_ID)) {
			if (action != null) {
				res.add(action.getActionType() != null ? action.getActionType().toString() : 
					OperationType.modify.toString());
			}
		} else {
			throw new RuntimeException("BUG! got request about attribute for which " +
					"we don't have value generator: " + attribute);
		}
		
		return res;
	}
}
