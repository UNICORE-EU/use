package eu.unicore.services.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.util.configuration.ConfigurationException;

public class MockAttributeSource implements IAttributeSource {

	@Override
	public void configure(String name) throws ConfigurationException {
	}

	@Override
	public void start(Kernel kernel) throws Exception {
	}

	@Override
	public String getStatusDescription() {
		return "OK";
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
