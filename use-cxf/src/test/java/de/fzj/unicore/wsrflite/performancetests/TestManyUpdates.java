package de.fzj.unicore.wsrflite.performancetests;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.registry.LocalRegistryClient;
import de.fzj.unicore.wsrflite.utils.StopWatch;
import de.fzj.unicore.wsrflite.xmlbeans.registry.LocalRegistryEntryHomeImpl;
import de.fzj.unicore.wsrflite.xmlbeans.registry.LocalRegistryHomeImpl;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryCreator;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryEntryUpdater;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryHandler;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import de.fzj.unicore.wsrflite.xmlbeans.sg.ServiceGroupEntry;
import eu.unicore.services.ws.WSServerResource;
import eu.unicore.services.ws.cxf.CXFServiceFactory;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;
import eu.unicore.services.ws.testutils.JettyTestCase;

public class TestManyUpdates extends JettyTestCase {

	@Before
	public void addServices() throws Exception{
		if(kernel.getService("example")!=null)return;
		CXFServiceFactory.createAndDeployService(kernel, "example", 
				WSServerResource.class, WSResourceHomeImpl.class, null);
		CXFServiceFactory.createAndDeployService(kernel, "ServiceGroupEntry", 
				ServiceGroupEntry.class, LocalRegistryEntryHomeImpl.class, null);
		CXFServiceFactory.createAndDeployService(kernel, "Registry", 
				Registry.class, LocalRegistryHomeImpl.class, null);
		kernel.setAttribute(RegistryHandler.class, new RegistryHandler(kernel));
		new RegistryCreator(kernel).createRegistry();
	}

	@Test
	public void refreshEntries() throws Exception {
		Logger.getLogger("unicore.services.registry").setLevel(Level.WARN);
		int N = 10;
		LocalRegistryClient registry = kernel.getAttribute(RegistryHandler.class).getRegistryClient();;
		List<String> items = new ArrayList<>();
		for(int i=0; i<N; i++){
			String ep = createInstance();
			String res = registry.addEntry(ep, new HashMap<>(),null);
			items.add(res);
		}
		RegistryEntryUpdater upd = new RegistryEntryUpdater();
		Home home = kernel.getHome("ServiceGroupEntry");

		Runtime.getRuntime().gc();
		long mem = Runtime.getRuntime().freeMemory();
		System.out.println("Free Mem: "+mem);
		
		int RUNS = 50;
		StopWatch sw = new StopWatch();
		sw.start("Starting "+RUNS+" update runs.");
		
		for(int r=0; r<RUNS; r++){
			int UPD = 100;
			System.out.println("Running <"+UPD+"> updates of <"+N+"> entries ... ");
			for(int i=0; i<UPD; i++){
				for(String id: items){
					upd.process(home, id);
				}
			}
			Runtime.getRuntime().gc();
			mem = Runtime.getRuntime().freeMemory();
			System.out.println("Free Mem: "+mem);
			sw.snapShot("Ending <"+(r+1)+">");
		}

	}

	private String createInstance() throws Exception{
		//directly add an instance
		Home h=kernel.getHome("example");
		assertNotNull(h);
		InitParameters init = new InitParameters();
		String uid=h.createResource(init);
		return getBaseurl()+"/example?res="+uid;
	}

}
