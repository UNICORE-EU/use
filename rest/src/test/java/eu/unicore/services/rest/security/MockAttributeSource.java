package eu.unicore.services.rest.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.configuration.ConfigurationException;

public class MockAttributeSource implements IAttributeSource {

	@Override
	public void configure(String name, Kernel kernel) throws ConfigurationException {
	}

	@Override
	public String getName() {
		return "Mock";
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder otherAuthoriserInfo)
			throws IOException {
		Map<String,String[]>attrs = new HashMap<>();
		if(!Client.ANONYMOUS_CLIENT_DN.equals(tokens.getEffectiveUserName())){
			attrs.put(IAttributeSource.ATTRIBUTE_ROLE, new String[]{"user"});
			attrs.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{"nobody"});	
		}
		return new SubjectAttributesHolder(attrs);
	}

}
