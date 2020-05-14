/*
 * Copyright (c) 2011-2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.ws.testutils;

import static org.junit.Assert.assertNotNull;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.junit.Before;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xfire.WSRFClientFactory;
import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.WSResource;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.exampleservice.ExampleFactoryImpl;
import eu.unicore.services.ws.exampleservice.IExampleFactory;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;
import eu.unicore.util.httpclient.IClientConfiguration;

public abstract class AbstractClientTest extends JettyTestCase {

	protected BaseWSRFClient client;
	protected IExampleFactory factoryClient;
	protected EndpointReferenceType epr;
	protected String url;
	
	@Before
	public void addServices() throws Exception{
		if(kernel.getService("serviceFactory")==null)
		{
			CXFServiceFactory.createAndDeployService(kernel,"service",
					WSResource.class,WSResourceHomeImpl.class,null);
			CXFServiceFactory.createAndDeployService(kernel,"serviceFactory",
					IExampleFactory.class,ExampleFactoryImpl.class,null);
		}
		createTestResource();
	}

	protected void createTestResource()throws Exception{
		String useUrl=getBaseurl();

		AddTestResourceDocument request=AddTestResourceDocument.Factory.newInstance();
		request.addNewAddTestResource();

		EndpointReferenceType epr1=EndpointReferenceType.Factory.newInstance();
		epr1.addNewAddress().setStringValue(useUrl+"/serviceFactory");
		WSRFClientFactory proxyMaker=new WSRFClientFactory(getClientSideSecurityProperties());
		factoryClient=(IExampleFactory)proxyMaker.createPlainWSProxy(
			IExampleFactory.class,
			useUrl+"/serviceFactory");
		
		AddTestResourceResponseDocument resp=factoryClient.addTestResource(request);
		assertNotNull(resp);
		epr=resp.getAddTestResourceResponse().getEndpointReference();
		System.out.println("Created test resource at "+epr.getAddress().getStringValue());
		String uid=WSUtilities.extractResourceID(epr);
		//test rp
		XmlCursor c=epr.addNewReferenceParameters().newCursor();
		c.toFirstContentToken();
		c.insertElementWithText(new QName("http://bar","baz"), uid);
		c.dispose();
		url=useUrl+"/service?res="+uid;
		client=createClient(getClientSideSecurityProperties());
	}

	protected BaseWSRFClient createClient(IClientConfiguration sec)throws Exception{
		return new BaseWSRFClient(url,epr,sec);
	}
}
