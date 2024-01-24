package eu.unicore.services.pdp.request.creator;

public class XACMLAttributeMeta implements Comparable<XACMLAttributeMeta> {

	public enum XACMLAttributeCategory {
		Environment, Subject, Resource, Action
	}

	private String name;
	private XACMLAttributeCategory category;
	private String type;
	private String value;

	public XACMLAttributeMeta(String xacmlName, String type,
			XACMLAttributeCategory category) {
		this.name = xacmlName;
		this.type = type;
		this.category = category;
	}

	public XACMLAttributeCategory getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public void setCategory(XACMLAttributeCategory category) {
		this.category = category;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int compareTo(XACMLAttributeMeta o) {
		return o.getName().compareTo(getName());
	}

	@Override
	public String toString()
	{
		return "XACMLAttributeMeta [name=" + name + ", category=" + category + ", type="
				+ type + ", value=" + value + "]";
	}
}
