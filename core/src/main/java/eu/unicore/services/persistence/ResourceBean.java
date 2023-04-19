/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

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
