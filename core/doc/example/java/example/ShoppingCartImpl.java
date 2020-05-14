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
 

package example;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.example.AddItemRequestDocument;
import org.example.AddItemResponseDocument;
import org.example.EntryDocument;
import org.example.ShoppingCartResourcePropertiesDocument;
import org.example.ItemDocument.Item;

import de.fzj.unicore.wsrflite.xmlbeans.impl.WSResourceImpl;

public class ShoppingCartImpl extends WSResourceImpl implements ShoppingCart {

   private static final long serialVersionUID=20934L;

   protected static Logger log=Logger.getLogger(ShoppingCartImpl.class.getName());
   
   @Override
   /**
    * by providing the QName of the resource property document, the document can be assembled
    * automatically. All that is needed in additions are java implementations of the resource 
    * roperties. They need to be added to the "properties" Map, using their QNames as keys.
    */
    public QName getResourcePropertyDocumentQName(){
		return ShoppingCartResourcePropertiesDocument.type.getDocumentElementName();
	}
   
	public void initialise(String serviceName, Map<String, Object> initobjs)throws Exception{
		super.initialise(serviceName, initobjs);
		log.info("Server: initialising service instance "+getUniqueID());
		
		// add java implementations of our resource properties
		ShoppingCartEntries scp=new ShoppingCartEntries();
		properties.put(RPEntryQName, scp);
		
		TotalPriceProperty tpp=new TotalPriceProperty(this);
		properties.put(RPTotalPriceQName, tpp);
		
	}

	/*
	 * implement Item addition
	 */
	public AddItemResponseDocument add(AddItemRequestDocument request) {
		System.out.println("Adding item to shopping cart: "+request);
		Item it=request.getAddItemRequest().getItem();
		((ShoppingCartEntries) properties.get(RPEntryQName) ).add(it);
		isDirty=true;
		AddItemResponseDocument res=AddItemResponseDocument.Factory.newInstance();
		res.addNewAddItemResponse();
		return res;
	}

	
	public ArrayList<EntryDocument> getEntries() {
		return ((ShoppingCartEntries) properties.get(RPEntryQName)).getEntries();
	}
	
}
