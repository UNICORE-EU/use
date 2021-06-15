package eu.unicore.uas.pdp.request.profile;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import eu.unicore.security.Client;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.pdp.ActionDescriptor;
import eu.unicore.services.security.SecurityManager;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta;
import eu.unicore.uas.pdp.request.creator.XACMLAttributeMeta.XACMLAttributeCategory;

public class EMI1Profile extends XACMLProfileBase {
	public static final String ATTR_PROFILE_ID_XACML_ID = "http://dci-sec.org/xacml/attribute/profile-id";
	public static final String ATTR_PROFILE_ID_VALUE = "http://dci-sec.org/xacml/profile/common-authz/1.1";

	public static final String ATTR_SUBJECT_ISSUER_XACML_ID = "http://dci-sec.org/xacml/attribute/subject-issuer";
	public static final String ATTR_VO_XACML_ID = "http://dci-sec.org/xacml/attribute/virtual-organization";
	public static final String ATTR_GROUP_XACML_ID = "http://dci-sec.org/xacml/attribute/group";
	public static final String ATTR_PRIMARY_GROUP_XACML_ID = "http://dci-sec.org/xacml/attribute/group/primary";
	public static final String ATTR_ROLE_XACML_ID = "http://dci-sec.org/xacml/attribute/role";
	public static final String ATTR_PRIMARY_ROLE_XACML_ID = "http://dci-sec.org/xacml/attribute/role/primary";
	public static final String ATTR_RESOURCE_OWNER_XACML_ID = "http://dci-sec.org/xacml/attribute/resource-owner";

	public EMI1Profile(String siteUrl) {
		super(siteUrl);
		this.siteUrl = siteUrl;
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_PROFILE_ID_XACML_ID,
				XACMLAttribute.Type.ANYURI.toString(), 
				XACMLAttributeCategory.Environment));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_SUBJECT_ISSUER_XACML_ID,
				XACMLAttribute.Type.X500NAME.toString(), 
				XACMLAttributeCategory.Subject));
		// attributes.put(ATTR_SUBJECT_KEY_INFO, new
		// XACMLAttributeMeta(ATTR_SUBJECT_KEY_INFO_I,ATTR_SUBJECT_KEY_INFO,ATTR_SUBJECT_KEY_INFO_TYPE,ATTR_SUBJECT_KEY_INFO_CATEGORY));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_VO_XACML_ID, 
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_GROUP_XACML_ID, 
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_PRIMARY_GROUP_XACML_ID,
				XACMLAttribute.Type.STRING.toString(),
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_ROLE_XACML_ID, 
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_PRIMARY_ROLE_XACML_ID,
				XACMLAttribute.Type.STRING.toString(),
				XACMLAttributeCategory.Subject));
		addAttributeDef(new XACMLAttributeMeta(
				XACMLAttribute.Name.XACML_RESOURCE_ID_ATTR.toString(),
				XACMLAttribute.Type.STRING.toString(), 
				XACMLAttributeCategory.Resource));
		addAttributeDef(new XACMLAttributeMeta(
				ATTR_RESOURCE_OWNER_XACML_ID,
				XACMLAttribute.Type.X500NAME.toString(), 
				XACMLAttributeCategory.Resource));
	}
	
	
	@Override
	public List<String> getValue(XACMLAttributeMeta attribute, Client c,
			ActionDescriptor action, ResourceDescriptor des) {
		List<String> res = new ArrayList<String>();
		String attrName = attribute.getName();
		
		if (attrName.equals(XACMLAttribute.Name.XACML_SUBJECT_ID_ATTR.toString())) {
			res.add(c.getDistinguishedName());
			
		} else if (attrName.equals(XACMLAttribute.Name.XACML_ACTION_ID_ATTR.toString())) {
			String actionN = action != null ? action.getAction() : null; 
			res.add(actionN != null ? actionN : SecurityManager.UNKNOWN_ACTION);
			
		} else if (attrName.equals(ATTR_PROFILE_ID_XACML_ID)) {
			res.add(EMI1Profile.ATTR_PROFILE_ID_VALUE);
			
		} else if (attrName.equals(ATTR_SUBJECT_ISSUER_XACML_ID)) {
			if (c.getSecurityTokens() != null) {
				X509Certificate userCert = c.getSecurityTokens()
						.getEffectiveUserCertificate();
				res.add(userCert.getIssuerX500Principal().getName());
			}
			
		} else if (attrName.equals(ATTR_VO_XACML_ID)) {
			res = getEMIVos(c.getVos());
			
		} else if (attrName.equals(ATTR_GROUP_XACML_ID)) {
			res = getEMIGroups(c.getVos());
			
		} else if (attrName.equals(ATTR_PRIMARY_GROUP_XACML_ID)) {
			if (c.getVo() != null)
				res.add(c.getVo());
			
		} else if (attrName.equals(ATTR_ROLE_XACML_ID)) {
			String[] roles = c.getRole().getValidRoles();
			if (roles != null)
				Collections.addAll(res, roles);
			
		} else if (attrName.equals(ATTR_PRIMARY_ROLE_XACML_ID)) {
			res.add(c.getRole().getName());
			
		} else if (attrName.equals(XACMLAttribute.Name.XACML_RESOURCE_ID_ATTR.toString())) {
			res.add(getU6ResourceAttr(siteUrl,
				des.getServiceName(), des.getResourceID()));
			
		} else if (attrName.equals(ATTR_RESOURCE_OWNER_XACML_ID)) {
			String n = des.getOwner();
			if (n != null)
				res.add(new X500Principal(n).getName());
		} else {
			throw new RuntimeException("BUG! got request about attribute for which " +
					"we don't have value generator: " + attribute);
		}

		return res;
	}
	
	protected String getU6ResourceAttr(String baseUrl, String serviceName,
			String wsrfId) {
		StringBuilder res = new StringBuilder();
		URL u;
		res.append("u6://");
		try {
			u = new URL(baseUrl);
			res.append(u.getHost() + ":" + u.getPort());
		} catch (MalformedURLException e) {
		}
		res.append("/" + serviceName);
		res.append("/" + wsrfId);
		return res.toString();
	}

	protected List<String> getEMIVos(String[] groups) {
		Set<String> vos = new HashSet<String>();
		for (String g : groups) {
			if (!g.startsWith("/"))
				g = "/" + g;
			String[] v = g.trim().split("/");
			vos.add(v[1]);
		}
		List<String> ans = new ArrayList<String>(vos);
		Collections.sort(ans);
		return ans;
	}

	protected List<String> getEMIGroups(String[] groups) {
		List<String> ans = new ArrayList<String>();
		Collections.addAll(ans, groups);
		Collections.sort(ans);
		return ans;
	}
}
