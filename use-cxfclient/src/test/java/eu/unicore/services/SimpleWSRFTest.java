package eu.unicore.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.ws.AdminActionRequestDocument;
import eu.unicore.services.ws.AdminActionResponseDocument;
import eu.unicore.services.ws.AdminActionValueType;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.GetServiceInstancesRequestDocument;
import eu.unicore.services.ws.ResourceProperties;
import eu.unicore.services.ws.WSRFClientFactory;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.services.ws.testutils.AbstractTestBase;
import eu.unicore.services.ws.testutils.JettyServer;
import eu.unicore.services.ws.testutils.MockSecurityConfig;
import eu.unicore.services.ws.testutils.SimpleService;
import eu.unicore.services.ws.testutils.SimpleServiceImpl;
import eu.unicore.util.Log;

/**
 * simple checks for WSRF style services
 */
public class SimpleWSRFTest extends AbstractTestBase{

	protected static final String serviceName="SimpleServiceImpl";
	
	protected static final QName serviceQName=new QName("foo", serviceName);

	@Test
	public void testWSA() throws Exception{
		addSimpleService();
		String url="https://localhost:" + JettyServer.PORT + "/services/SimpleServiceImpl?res=fake_res_id";
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(url);

		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		sec.setMessageLogging(false);
		SimpleService client = new WSRFClientFactory(sec).createProxy(SimpleService.class, url, epr);
		doWSATest(client, epr, true);
	}
	
	@Test
	public void testWSAMulti() throws Exception{
		addSimpleService();
		String url="https://localhost:" + JettyServer.PORT + "/services/SimpleServiceImpl?res=fake_res_id";
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(url);

		MockSecurityConfig sec = new MockSecurityConfig(true, true);
		SimpleService client = new WSRFClientFactory(sec).createProxy(SimpleService.class, url, epr);
		for(int i=0;i<50;i++){
			System.out.print(".");
			doWSATest(client, epr, false);
		}
		System.out.println();
	}
	
	private void doWSATest(SimpleService client, EndpointReferenceType epr, boolean log)throws Exception{
		String msg="Hello";
		GetServiceInstancesRequestDocument in=GetServiceInstancesRequestDocument.Factory.newInstance();
		in.addNewGetServiceInstancesRequest().setServiceName(msg);
		String reply=client.foo(in).getGetServiceInstancesResponse().getServiceNamespace();
		assertEquals(msg, reply);
		
		AdminActionRequestDocument in2=AdminActionRequestDocument.Factory.newInstance();
		AdminActionValueType param = in2.addNewAdminActionRequest().addNewParameter();
		param.setName("log");
		param.setName(String.valueOf(log));
		AdminActionResponseDocument response=client.bar(in2);
		String wsa=response.getAdminActionResponse().getResultsArray()[0].getValue();
		assertEquals(epr.getAddress().getStringValue(), wsa);
	}
	
	private void addSimpleService()throws Exception{
		addService(serviceName, serviceQName, SimpleServiceImpl.class);
	}
	
	@Test
	public void testRP()throws Exception{
		System.out.println("*** testRP()");
		String url="https://localhost:" + JettyServer.PORT + "/services/MockServiceImpl?res=fake_res_id";
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(url);

		MockSecurityConfig sec = new MockSecurityConfig(true, true);

		ResourceProperties client = new WSRFClientFactory(sec).createProxy(ResourceProperties.class, url, epr);
		GetResourcePropertyDocument in=GetResourcePropertyDocument.Factory.newInstance();
		in.setGetResourceProperty(mockServiceQName);
		try{
		XmlObject reply=client.GetResourceProperty(in);
		System.out.println(reply);
		}catch(Exception ex){
			System.out.println(Log.createFaultMessage("", ex));
			ex.printStackTrace();
		}
		DeleteResourcePropertiesDocument in1=DeleteResourcePropertiesDocument.Factory.newInstance();
		in1.addNewDeleteResourceProperties().addNewDelete();
		try{
			client.DeleteResourceProperties(in1);
		}
		catch(Exception ex){
			System.out.println(Log.createFaultMessage("got exception:", ex));
			assertTrue(ex instanceof BaseFault);
		}
	}

}


