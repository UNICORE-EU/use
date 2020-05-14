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

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.example.AddItemRequestDocument;
import org.example.CreateShoppingCartDocument;
import org.example.CreateShoppingCartResponseDocument;
import org.example.ItemDocument.Item;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.jetty.JettyServer;
import de.fzj.unicore.wsrflite.persistence.HsqldbPersist;
import de.fzj.unicore.wsrflite.persistence.IPersistenceProperties;
import de.fzj.unicore.wsrflite.xfire.JettyTestCase;
import de.fzj.unicore.wsrflite.xfire.XFireClientFactory;
import de.fzj.unicore.wsrflite.xfire.XFireKernel;
import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;

public class TestShop extends JettyTestCase{
	protected static BaseWSRFClient client;
	protected static String cartUrl;
	protected static JettyServer server;
	protected static EndpointReferenceType epr;
	
	static boolean haveInit=false;
	
	protected void setUp()throws Exception{
		if(haveInit==false){
			System.setProperty(HsqldbPersist.clearDBOnStartup,"true");
			super.setUp();
			addServices();
			
			//logging
			LogManager.getLogManager().reset();
			ConsoleHandler c=new ConsoleHandler();
			c.setLevel(Level.OFF);
			Logger.getLogger("de").addHandler(c);
			
			//create a proxy for the IShop service
			IShop s= (IShop)(new XFireClientFactory()).createPlainWSProxy
				(IShop.class, getBaseurl()+"/shop",null);
			
			
			//create a request and send it via the proxy
			CreateShoppingCartDocument request=CreateShoppingCartDocument.Factory.newInstance();
			request.addNewCreateShoppingCart();
			CreateShoppingCartResponseDocument resp=s.createNewShoppingCart(request);
			
			//extract the epr from the result
			epr=resp.getCreateShoppingCartResponse().getEndpointReference();
			System.out.println("new resource at "+epr);
			cartUrl=getBaseurl()+"/cart";
			client=new BaseWSRFClient(cartUrl,epr);
			//do not cache stuff at the client
			client.setUpdateInterval(-1);
			haveInit=true;
			Thread.sleep(400);
		}
	}

	protected void addServices() throws Exception {
		Kernel.getKernel().setProperty(IPersistenceProperties.WSRF_PERSIST_CLASSNAME,HsqldbPersist.class.getName());
		Kernel.getKernel().setProperty(IPersistenceProperties.WSRF_PERSIST_STORAGE_DIRECTORY,".");
		
		//the factory service that can create new carts
		XFireKernel.exposeAsService("shop", IShop.class, ShopImpl.class, false);
		//the shopping cart service
		XFireKernel.exposeAsService("cart", ShoppingCart.class, ShoppingCartHomeImpl.class, true);
		
		
	}
	
	public void test()throws Exception{
	
		System.out.println();
		System.out.println("Shopping cart status:");
		System.out.println();
		System.out.println(client.getResourcePropertyDocument());
		
		Item item=org.example.ItemDocument.Item.Factory.newInstance();
		item.setName("Goleo VI");
		item.setID("1");
		System.out.println();
		System.out.println("Add new item: "+item);
		System.out.println();
		
		//create a proxy for calling the service
		
		ShoppingCart cart=(ShoppingCart)client.makeProxy(ShoppingCart.class);
		
		//use the "add" method to add an item
		AddItemRequestDocument req=AddItemRequestDocument.Factory.newInstance();
		req.addNewAddItemRequest().setItem(item);
		// add the item twice
		cart.add(req);
		cart.add(req);
		
		
		//check resource properties now...
		System.out.println();
		System.out.println("Shopping cart status:");
		System.out.println();
		System.out.println(client.getResourcePropertyDocument());
		
		Item item2=Item.Factory.newInstance();
		item2.setName("T-Shirt Italy");
		item2.setID("2");
		System.out.println();
		System.out.println("Add new item: "+item2);
		System.out.println();
		req=AddItemRequestDocument.Factory.newInstance();
		req.addNewAddItemRequest().setItem(item2);
		cart.add(req);

		System.out.println();
		System.out.println("Shopping cart status:");
		System.out.println();
		System.out.println(client.getResourcePropertyDocument());
		
		
	}
	
}
