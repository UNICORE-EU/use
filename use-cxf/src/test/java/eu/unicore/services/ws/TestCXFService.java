package eu.unicore.services.ws;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;

import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.exampleservice.ExampleService;
import eu.unicore.services.ws.exampleservice.IExampleService;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestCXFService extends JettyTestCase{

	@Before
	public void addServices() throws Exception{
		if(kernel.getService("service")!=null){
			return;
		}
		CXFServiceFactory.createAndDeployService(kernel, 
				"service",
				IExampleService.class,
				ExampleService.class,null);

		CXFServiceFactory.createAndDeployService(kernel, 
				"service2",
				IExampleService.class,
				ExampleService.class,null);
	}

	@Test
	public void testGetWSDL()throws Exception{

		//call to get the wsdl
		URL url=new URL(getBaseurl()+"/service?wsdl");
		BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));
		String b;
		do{
			b=br.readLine();
			//if(b!=null)System.out.println(b);
		}
		while(b!=null);
		br.close();

	}

	@Test
	public void testInvokeServices()throws Exception{

		String url=getBaseurl()+"/service";
		IExampleService s= new WSClientFactory(getClientSideSecurityProperties())
		.createPlainWSProxy(IExampleService.class, url);

		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(new QName("foo", "bar"));
		GetResourcePropertyResponseDocument res=s.GetResourceProperty(req);
		assertTrue(res.toString().contains("Hello"));

		String url2=getBaseurl()+"/service2";
		IExampleService s2= new WSClientFactory(getClientSideSecurityProperties())
		.createPlainWSProxy(IExampleService.class, url2);

		GetResourcePropertyDocument req2=GetResourcePropertyDocument.Factory.newInstance();
		req2.setGetResourceProperty(new QName("foo", "bar"));
		GetResourcePropertyResponseDocument res2=s2.GetResourceProperty(req2);
		assertTrue(res2.toString().contains("Hello"));

	}

	@Test
	public void testInvokeWithNullParam()throws Exception{

		String url=getBaseurl()+"/service";
		IExampleService s= new WSClientFactory(getClientSideSecurityProperties())
		.createPlainWSProxy(IExampleService.class, url);

		SetTerminationTimeDocument req=SetTerminationTimeDocument.Factory.newInstance();
		req.addNewSetTerminationTime().setNil();

		System.out.println(req);
		s.throwBaseFault(req);
	}

}
