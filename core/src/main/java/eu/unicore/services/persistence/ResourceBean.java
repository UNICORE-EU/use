package eu.unicore.services.persistence;

import java.io.Serializable;
import java.util.Set;

import eu.unicore.persist.annotations.Column;
import eu.unicore.persist.annotations.ID;
import eu.unicore.persist.util.JSON;
import eu.unicore.services.Model;

/**
 * information that gets persisted
 */
@JSON(customHandlers={
		ModelWrapper.Adapter.class,
		ModelWrapper.XBeanAdapter.class
})
public class ResourceBean implements Serializable {
	
	private static final long serialVersionUID=1L;
	
	@ID
	public String uniqueID;
	
	@Column(name="tags")
	public String tags;
	
	public String serviceName;
	
	public String className;
	
	private ModelWrapper state;
	
	public ResourceBean(String uniqueID, String serviceName, String className, Model state){
		this.uniqueID=uniqueID;
		this.serviceName=serviceName;
		this.state=new ModelWrapper(state);
		this.tags = state!=null ? encodeTags(state.getTags()) : null;
		this.className=className;
	}
	
	public Model getState(){
		return state.getModel();
	}

	//needed by persistence lib
	public String getUniqueID(){
		return uniqueID;
	}

	public String getTags(){
		return tags;
	}
	
	protected String encodeTags(Set<String>tags){
		StringBuilder sb = new StringBuilder();
		for(String tag: tags){
			tag = tag.trim();
			if(tag.isEmpty())continue;
			sb.append(",").append(tag).append(",");
		}
		return sb.toString();
	}
}
