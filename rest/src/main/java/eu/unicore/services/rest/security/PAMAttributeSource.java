package eu.unicore.services.rest.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.util.configuration.ConfigurationException;

public class PAMAttributeSource implements IAttributeSource {

	public static final String PAM_ATTRIBUTES = "_PAM_ATTRIBUTES";
	
	@Override
	public void configure(String name) throws ConfigurationException {}

	@Override
	public void start(Kernel kernel) throws Exception {}

	@Override
	public String getStatusDescription() {
		return "PAM Attribute Source: OK";
	}

	@Override
	public String getName() {
		return "PAM";
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder otherAuthoriserInfo)
			throws IOException {
		PAMAttributes attrs = (PAMAttributes)tokens.getContext().get(PAM_ATTRIBUTES);
		Map<String, String[]> ret = new HashMap<String, String[]>();
		if (attrs != null)
			putAttributes(attrs, ret);
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
