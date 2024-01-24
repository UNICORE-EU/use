package eu.unicore.services.aip.saml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.security.util.AttributeHandlingCallback;

/**
 * Puts SAML attributes (in UVOS Attribute form(at)) into Client. Rather not used anywhere
 * but just in case. 
 * @author K. Benedyczak
 */
public class AttributesCallback implements AttributeHandlingCallback
{
	private String id;
	private String name;
	
	public AttributesCallback(String id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributesCallback other = (AttributesCallback) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> extractAttributes(SecurityTokens tokens)
	{
		ThreadContext.push(name);
		try
		{
			Map<String, Object> ctx = tokens.getContext();
			if (ctx == null)
				return null;
			HashMap<String, List<ParsedAttribute>> authzAttribsMap = 
					(HashMap<String, List<ParsedAttribute>>) ctx.get(id);
			if (authzAttribsMap == null)
				return null;
			// TODO this is probably useless, but we cannot store arbitrary
			// objects, it would mess up persistence
			Map<String, String> ret = new HashMap<>();
			ret.put(id, authzAttribsMap.toString());
			return ret;
		} finally
		{
			ThreadContext.pop();
		}
	}
}
