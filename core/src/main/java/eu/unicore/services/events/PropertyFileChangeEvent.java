package eu.unicore.services.events;

import java.util.Properties;

//FIXME - this is poor. Should provide a better interface (notifications per-property), 
//on a lower level, and available for all kinds of configurations, not only wsrflite.xml
public class PropertyFileChangeEvent implements Event {
	
	private final String file;
	private final Properties properties;
	
	public PropertyFileChangeEvent(String file, Properties newProperties){
		this.file=file;
		properties = newProperties;
	}
	
	public String getFile(){
		return file;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}
}
