/*********************************************************************************
 * Copyright (c) 2006-2008 Forschungszentrum Juelich GmbH 
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
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.example.EntryDocument;
import org.example.TotalPriceDocument;
import org.example.ItemDocument.Item;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;

import de.fzj.unicore.wsrflite.WSRFInstance;
import de.fzj.unicore.wsrflite.utils.Utilities;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperty;
import de.fzj.unicore.wsrflite.xmlbeans.WSResource;

public class TotalPriceProperty extends ResourceProperty<Object>{
    protected static final long serialVersionUID=20394L;
	
    private TotalPriceDocument[] xml;
    
    public TotalPriceProperty(WSRFInstance res) {
    	// reference to the resource is needed in order to access the entry resource property
		this.parentWSResource=res;
    	xml = new TotalPriceDocument[1];
    	TotalPriceDocument doc = TotalPriceDocument.Factory.newInstance();
    	doc.setTotalPrice(0);
    	xml[0] = doc;
    }
    

	public TotalPriceDocument[] getXml() {
		return xml;
	}
	
	@Override
	public TotalPriceProperty update(){
		try {
			// retrieve entry resource property
			GetResourcePropertyDocument req = GetResourcePropertyDocument.Factory.newInstance();
			req.setGetResourceProperty(ShoppingCart.RPEntryQName);
			GetResourcePropertyResponseDocument resp = ((WSResource) parentWSResource).GetResourceProperty(req);
			// walk through entries and calculate the sum of prices
			XmlObject[] xmls = Utilities.extractAnyElements(resp.getGetResourcePropertyResponse(),ShoppingCart.RPEntryQName);
			float price=0;
			for (int i = 0; i < xmls.length; i++) {
				EntryDocument entryDoc = EntryDocument.Factory.parse(xmls[i].xmlText());
				Item item = entryDoc.getEntry().getItem();
				price+=lookupPrice(item)*entryDoc.getEntry().getNumber();
			}
			xml[0].setTotalPrice(price);
		} catch (Exception e) {
			System.out.println("Problem updating TotalPrice resource property, some related information is unavailable:");
			e.printStackTrace();
			xml[0] = null;
		}
		return this;
	}
	
	/**
	 * compute the total price 
	 */
	public float getTotalPrice(){
		System.out.println("+++ computing total price");
		float res=0;
		List<EntryDocument>entries=((ShoppingCartImpl)parentWSResource).getEntries();
		for(EntryDocument entry: entries){
			res+=lookupPrice(entry.getEntry().getItem());
		}
		return res;
	}
	

	protected float lookupPrice(Item item){
		return 1.0f; //TODO
	}
	
}
