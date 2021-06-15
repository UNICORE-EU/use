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
 

package eu.unicore.services;

import java.io.Serializable;

/**
 * A resource property encapsulates some state, 
 * making it possible to expose this state as a WS-Resource.
 * 
 * @author schuller
 */
public abstract class ResourcePropertyBase<XMLType,BackingType> implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * a reference to the "parent" resource
	 */
	protected transient Resource parentWSResource;
	
	public ResourcePropertyBase(Resource resource){
		this.parentWSResource=resource;
	}
	
	public void setResource(Resource resource){
		this.parentWSResource=resource;
	}
	/**
	 *  update the property
	 * 	@return the updated property (i.e. this)
	 */
	public ResourcePropertyBase<XMLType,?> update() throws Exception{
		return this;
	}
	
	/**
	 * get the XML representation of this property.
	 * 
	 * @return an array of XmlObject
	 */
	public abstract XMLType[] getXml();
	
	/**
	 * get the property as a java object
	 * (optional, return null if not applicable)
	 * @return the "backing" Object
	 */
	public BackingType getProperty(){
		return null;
	}
	
	/**
	 * set the property as a java object
	 */
	public void setProperty(BackingType setTo){
		
	}
	
	/**
	 * is this a readonly property?
	 * @return true if the property is read-only
	 */
	public boolean isReadOnly(){
		return true;
	}
}
