package eu.unicore.services.registry;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.services.impl.BaseModel;

public class RegistryEntryModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	private Map<String,String>content = new HashMap<>();

	public String getEndpoint() {
		return content.get(RegistryImpl.ENDPOINT);
	}

	public Map<String,String> getContent() {
		return content;
	}

	public void setContent(Map<String,String> content) {
		this.content = content;
	}

}
