package eu.unicore.services.aip.saml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnicoreIncarnationAttributes
{
	private Map<String, String[]> validAttributes;
	private Map<String, String[]> defaultAttributes;
	private Map<String, String[]> defaultVoAttributes;

	public UnicoreIncarnationAttributes()
	{
	}

	public UnicoreIncarnationAttributes(
			Map<String, String[]> validAttributes,
			Map<String, String[]> defaultAttributes,
			Map<String, String[]> defaultVoAttributes)
	{
		this.validAttributes = validAttributes;
		this.defaultAttributes = defaultAttributes;
		this.defaultVoAttributes = defaultVoAttributes;
	}

	public void setFromLists(
			Map<String, List<String>> validAttributes,
			Map<String, List<String>> defaultAttributes,
			Map<String, List<String>> defaultVoAttributes)
	{
		this.validAttributes = convertMap(validAttributes);
		this.defaultAttributes = convertMap(defaultAttributes);
		this.defaultVoAttributes = convertMap(defaultVoAttributes);
	}
	
	private Map<String, String[]> convertMap(Map<String, List<String>> input)
	{
		Map<String, String[]> ret = new HashMap<>();
		var iterator = input.entrySet().iterator();
		while(iterator.hasNext())
		{
			var e = iterator.next();
			String[] v = e.getValue().toArray(new String[e.getValue().size()]); 
			ret.put(e.getKey(), v);
		}
		return ret;
	}
	

	public Map<String, String[]> getValidAttributes()
	{
		return validAttributes;
	}

	public void setValidAttributes(Map<String, String[]> validAttributes)
	{
		this.validAttributes = validAttributes;
	}

	public Map<String, String[]> getDefaultAttributes()
	{
		return defaultAttributes;
	}

	public void setDefaultAttributes(Map<String, String[]> defaultAttributes)
	{
		this.defaultAttributes = defaultAttributes;
	}

	public Map<String, String[]> getDefaultVoAttributes()
	{
		return defaultVoAttributes;
	}

	public void setDefaultVoAttributes(Map<String, String[]> defaultVoAttributes)
	{
		this.defaultVoAttributes = defaultVoAttributes;
	}
}
