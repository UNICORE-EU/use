package eu.unicore.services.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * This attribute source collects basic attributes (xlogin and group) that might 
 * have been already set by the authentication process.
 *
 * For example, when authenticating via PAM username/password, the username and group
 * is already established.
 *
 * The role is set to "user".
 *
 * This attribute source is enabled by default, and depending on the configured attribute merging policy,
 * is first or last in the chain, so other attribute sources can override the attributes collected here.
 *
 * @author schuller
 */
public class AuthAttributesCollector implements IAttributeSource {

	public static final String ATTRIBUTES = "_PAM_ATTRIBUTES";
	
	@Override
	public void configure(String name) throws ConfigurationException {}

	@Override
	public void start(Kernel kernel) throws Exception {}

	@Override
	public String getName() {
		return "PAM";
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder otherAuthoriserInfo)
			throws IOException {
		PAMAttributes attrs = (PAMAttributes)tokens.getContext().get(ATTRIBUTES);
		Map<String, String[]> ret = new HashMap<>();
		if (attrs != null)putAttributes(attrs, ret);
		return new SubjectAttributesHolder(null, ret, ret);
	}

	private void putAttributes(PAMAttributes attrs, Map<String, String[]> ret)
	{
		ret.put(IAttributeSource.ATTRIBUTE_ROLE, new String[]{"user"});
		ret.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{attrs.uid});
		if(attrs.groups!=null){
			ret.put(IAttributeSource.ATTRIBUTE_GROUP, attrs.groups);
		}
	}

	public static class PAMAttributes {
		public String uid;
		public String[] groups;
	}
	
}
