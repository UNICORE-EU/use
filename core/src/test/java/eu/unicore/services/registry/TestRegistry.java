package eu.unicore.services.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.services.security.TestConfigUtil;

public class TestRegistry {

	Kernel kernel;

	long defaultEntryLifetime;

	@Test
	public void testRegistry() throws Exception {
		kernel = new Kernel(TestConfigUtil.getInsecureProperties());
		setupServices(kernel);
		RegistryCreator rc = RegistryCreator.get(kernel);
		rc.createRegistry();

		// create entry
		Map<String,String>content = new HashMap<>();
		content.put("foo", "bar");
		String endpoint = "https://test123.info";

		String uid = update(endpoint, content); 
		assertNotNull(uid);

		RegistryEntryHome entryHome = (RegistryEntryHome)kernel.getHome(RegistryEntryImpl.SERVICENAME);
		RegistryEntryImpl entry = (RegistryEntryImpl)entryHome.get(uid);
		assertEquals(endpoint, entry.getModel().getEndpoint());
		
		Map<String, String> content2 = entry.getModel().getContent();
		assertEquals("bar", content2.get("foo"));
		System.out.println("Entry for <"+entry.getModel().getEndpoint()+"> : "+content2);

		// update entry
		content.put("foo", "spam");
		String uid2 = update(endpoint, content);
		assertEquals(uid, uid2);
		entry = (RegistryEntryImpl)entryHome.get(uid);
		assertEquals(endpoint, entry.getModel().getEndpoint());

		content2 = entry.getModel().getContent();
		assertEquals("spam", content2.get("foo"));
		System.out.println("Entry for <"+entry.getModel().getEndpoint()+"> : "+content2);

		LocalRegistryClient lrc = new LocalRegistryClient(kernel);
		System.out.println(lrc.listEntries());

	}

	private String update(String endpoint, Map<String,String>content) throws Exception {
		try(RegistryImpl registry = (RegistryImpl)kernel.getHome("Registry").getForUpdate("default_registry")){
			registry.addEntry(endpoint, content, null);
			return registry.getModel().getEntryID(endpoint);
		}
	}

	protected void setupServices(Kernel kernel) throws Exception {
		kernel.getDeploymentManager().deployService(new RegistryService());
		kernel.getDeploymentManager().deployService(new RegistryEntryService());
		kernel.start();
		ContainerProperties cfg = kernel.getContainerProperties(); 
		defaultEntryLifetime = cfg.getLongValue(ContainerProperties.WSRF_SGENTRY_TERMINATION_TIME);
	}

	public class RegistryHome extends DefaultHome {

		protected RegistryImpl doCreateInstance(){
			return new RegistryImpl();
		}

	}

	public class RegistryService implements Service {

		private final RegistryHome home = new RegistryHome();

		@Override
		public String getName() {
			return "Registry";
		}

		@Override
		public String getType() {
			return "test";
		}

		@Override
		public void start() throws Exception {
			home.setKernel(kernel);
			home.start(getName());
		}

		@Override
		public void stop() throws Exception {
			home.shutdown();
		}

		@Override
		public void stopAndCleanup() throws Exception {
		}

		@Override
		public boolean isStarted() {
			return false;
		}

		@Override
		public Home getHome() {
			return home;
		}

		@Override
		public String getInterfaceClass() {
			return null;
		}

	}

	public class RegistryEntryHome extends DefaultHome {

		protected RegistryEntryImpl doCreateInstance(){
			return new RegistryEntryImpl();
		}

	}

	public class RegistryEntryService implements Service {

		private final RegistryEntryHome home = new RegistryEntryHome();

		@Override
		public String getName() {
			return "ServiceGroupEntry";
		}

		@Override
		public String getType() {
			return "test";
		}

		@Override
		public void start() throws Exception {
			home.setKernel(kernel);
			home.start(getName());
		}

		@Override
		public void stop() throws Exception {
			home.shutdown();
		}

		@Override
		public void stopAndCleanup() throws Exception {
		}

		@Override
		public boolean isStarted() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Home getHome() {
			return home;
		}

		@Override
		public String getInterfaceClass() {
			return null;
		}

	}
}
