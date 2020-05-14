package eu.unicore.services.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.persistence.Persistence;
import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestWSRFService extends JettyTestCase{
	
	@Before
	public void addServices() throws Exception{
		if(kernel.getService("example")!=null)return;
		CXFServiceFactory.createAndDeployService(kernel, "example", 
				WSServerResource.class, WSResourceHomeImpl.class, null);
	}
	
	@Override
	protected Properties getServerSideSecurityProperties() throws Exception {
		Properties properties = super.getServerSideSecurityProperties();
		properties.put("container."+ContainerProperties.WSRF_PERSIST_CLASSNAME,Persistence.class.getName());
		return properties;
	}
	
	@Test
	public void testConcurrentAccess()throws Exception {
		MockResource.slow=true;
		Map<String,Long> results = makeClientCalls(true);
		long r1_duration = results.get("r1_duration");
		long r2_duration = results.get("r2_duration");
		System.out.println("Call 1 took: "+r1_duration);
		System.out.println("Call 2 took: "+r2_duration);
		MockResource.slow=false;
		assertTrue(Math.abs(r1_duration-r2_duration)<1000);
	}

	@Test
	public void testSerialAccess()throws Exception {
		MockResource.slow=true;
		Map<String,Long> results = makeClientCalls(false);
		
		long r1_duration = results.get("r1_duration");
		long r2_duration = results.get("r2_duration");
		System.out.println("Call 1 took: "+r1_duration);
		System.out.println("Call 2 took: "+r2_duration);
		MockResource.slow=false;
		assertTrue(Math.abs(r1_duration-r2_duration)>MockResource.sleep/2);
	}

	
	protected Map<String,Long>makeClientCalls(final boolean concurrent)throws Exception {
		Map<String,Long> results = new HashMap<String, Long>();
		final BaseWSRFClient client=createInstance();
		
		final GetResourcePropertyDocument in=GetResourcePropertyDocument.Factory.newInstance();
		in.setGetResourceProperty(TerminationTimeDocument.type.getDocumentElementName());
		
		ExecutorService es=kernel.getContainerProperties().getThreadingServices().getExecutorService();
		
		final AtomicBoolean r1_finished = new AtomicBoolean();
		final AtomicBoolean r2_finished = new AtomicBoolean();
		final AtomicLong r1_start=new AtomicLong();
		final AtomicLong r1_end=new AtomicLong();
		final AtomicLong r2_start=new AtomicLong();
		final AtomicLong r2_end=new AtomicLong();
		
		Runnable r1=new Runnable(){
			public void run(){
				r1_start.set(System.currentTimeMillis());
				try{
					if(concurrent){
						client.getRP().GetResourceProperty(in);
					}
					else{
						Calendar tt=Calendar.getInstance();
						tt.add(Calendar.DATE,1);
						client.setTerminationTime(tt);
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
				r1_end.set(System.currentTimeMillis());
				r1_finished.set(true);
			}
		};
		
		Runnable r2=new Runnable(){
			public void run(){
				r2_start.set(System.currentTimeMillis());
				try{
					if(concurrent){
						client.getRP().GetResourceProperty(in);
					}
					else{
						Calendar tt=Calendar.getInstance();
						tt.add(Calendar.DATE,1);
						client.setTerminationTime(tt);
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
				r2_end.set(System.currentTimeMillis());
				r2_finished.set(true);
			}
		};
		
		es.execute(r1);
		es.execute(r2);
		
		while(!r1_finished.get()){
			Thread.sleep(1000);
		}
		while(!r2_finished.get()){
			Thread.sleep(1000);
		}
		
		client.destroy();
		
		long r1_duration = r1_end.get()-r1_start.get();
		long r2_duration = r2_end.get()-r2_start.get();
				
		results.put("r1_duration",r1_duration);
		results.put("r2_duration",r2_duration);
		return results;
	}

	private BaseWSRFClient createInstance() throws Exception{
		//directly add an instance
		Home h=kernel.getHome("example");
		assertNotNull(h);
		InitParameters init = new InitParameters("test123");
		String uid=h.createResource(init);
		assertEquals("test123",uid);
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(getBaseurl()+"/example?res="+uid);
		return new BaseWSRFClient(epr, kernel.getClientConfiguration().clone());
	}

}
