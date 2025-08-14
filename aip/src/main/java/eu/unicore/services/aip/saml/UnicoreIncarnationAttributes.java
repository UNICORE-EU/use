package eu.unicore.services.aip.saml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnicoreIncarnationAttributes
{
	private final Map<String, String[]> validAttributes;
	private final Map<String, String[]> defaultAttributes;
	private final Map<String, String[]> defaultVoAttributes;

	public UnicoreIncarnationAttributes(
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

	public Map<String, String[]> getDefaultAttributes()
	{
		return defaultAttributes;
	}

	public Map<String, String[]> getDefaultVoAttributes()
	{
		return defaultVoAttributes;
	}
}
