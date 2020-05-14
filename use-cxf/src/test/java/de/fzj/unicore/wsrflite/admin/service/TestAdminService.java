package de.fzj.unicore.wsrflite.admin.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.AdminService;
import de.fzj.unicore.wsrflite.xmlbeans.MetricValueDocument.MetricValue;
import de.fzj.unicore.wsrflite.xmlbeans.ServiceEntryDocument.ServiceEntry;
import de.fzj.unicore.wsrflite.xmlbeans.client.AdminServiceClient;
import de.fzj.unicore.wsrflite.xmlbeans.client.AdminServiceClient.AdminActionResult;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestAdminService extends JettyTestCase {

	@Before
	public void addServices() throws Exception {
		if(kernel.getService(AdminService.SERVICE_NAME)!=null)return;
		
		CXFServiceFactory.createAndDeployService(kernel, AdminService.SERVICE_NAME,
				AdminService.class,AdminServiceHomeImpl.class, null);
	}

	@Test
	public void testGetRPDocument()throws Exception{
		AdminServiceClient client=getClient();
		System.out.println(client.getResourcePropertyDocument());
	}

	@Test
	public void testGetServiceNames()throws Exception {
		AdminServiceClient client = getClient();
		ServiceEntry[] serviceEntries = client.getServiceNames();
		assertTrue(serviceEntries.length>0);
	}

	@Test
	public void testGetServiceInstances() throws Exception {		
		AdminServiceClient client = getClient();
		client.getServiceInstances(AdminService.SERVICE_NAME).getGetServiceInstancesResponse().getUidArray();
	}

	
	@Test
	public void testGetAllMetrics() throws Exception {
		AdminServiceClient client = getClient();
		MetricValue[] values = client.getMetrics((String[])null);
		assertTrue(values.length>0);
		for(MetricValue value:values) {
			System.out.println(value);
		}
	}

	@Test
	public void testGetFilteredMetrics() throws Exception {
		AdminServiceClient client = getClient();
		String[] names=new String[]{"use.wsrf.callFrequency"};
		MetricValue[] values=client.getMetrics(names);
		assertEquals(1,values.length);
	}
	
	@Test
	public void testGetSingleMetric() throws Exception {
		AdminServiceClient client = getClient();
		MetricValue value = client.getMetric("use.wsrf.callFrequency");
		assertNotNull(value);
		System.out.println(value);
		value = client.getMetric("no_such_metric");
		assertNull(value);
	}

	@Test
	public void testListAdminActions()throws Exception{
		AdminServiceClient client = getClient();
		List<AdminServiceClient.AdminAction> a=client.getAdminActions();
		assertNotNull(a);
		assertTrue(a.size()>0);
		assertTrue(a.contains(new AdminServiceClient.AdminAction("mock", "n/a")));
		System.out.println("Have admin action: "+a.get(0).name+" ("+a.get(0).description+")");
	}
	
	@Test
	public void testInvokeAdminAction()throws Exception{
		AdminServiceClient client = getClient();
		Map<String,String>params=new HashMap<String,String>();
		params.put("k1","v1");
		params.put("k2","v2");
		AdminActionResult result=client.invokeAdminAction("mock", params);
		assertTrue(result.successful());
		assertEquals("ok",result.getMessage());
		assertEquals(2, result.getResults().size());
		assertEquals("echo-v1",result.getResults().get("k1"));
	}

	private AdminServiceClient getClient() throws Exception {
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		String uri = getBaseurl()+"/"+AdminService.SERVICE_NAME+"?res="+AdminService.SINGLETON_ID;
		epr.addNewAddress().setStringValue(uri);

		AdminServiceClient c= new AdminServiceClient(epr, getClientSideSecurityProperties());
		c.setUpdateInterval(-1);
		return c;
	}

}
