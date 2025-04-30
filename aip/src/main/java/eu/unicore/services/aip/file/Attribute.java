package eu.unicore.services.aip.file;

import java.util.List;

/**
 * Represents one attribute read form the attributes files i.e. name and value(s).
 * @author golbi
 */
public class Attribute
{

	private final String name;

	private final List<String> values;

	public Attribute(String name, List<String> values)
	{
		this.name = name;
		this.values = values;
	}

	public String getName()
	{
		return name;
	}

	public List<String> getValues()
	{
		return values;
	}


	@Override
	public String toString() {
		return name+": "+String.valueOf(values);
	}
}
