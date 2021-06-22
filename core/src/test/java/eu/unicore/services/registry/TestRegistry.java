package eu.unicore.services.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
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

		RegistryHome home = (RegistryHome)kernel.getHome("Registry");
		InitParameters initParams = new InitParameters("default_registry", TerminationMode.NEVER);
		home.createResource(initParams);

		// create entry
		Map<String,String>content = new HashMap<>();
		content.put("foo", "bar");
		String endpoint = "https://test123.info";

		String uid = update(endpoint, content); 
		assertNotNull(uid);

		RegistryEntryHome entryHome = (RegistryEntryHome)kernel.getHome(RegistryEntryImpl.SERVICENAME);
		RegistryEntryImpl entry = (RegistryEntryImpl)entryHome.get(uid);
		assertEquals(endpoint, entry.getModel().getEndpoint());
		
		RegistryImpl registry = (RegistryImpl)home.get("default_registry");
		Map<String, String> content2 = registry.getModel().getContent(endpoint);
		assertEquals("bar", content2.get("foo"));
		System.out.println("Entry for <"+entry.getModel().getEndpoint()+"> : "+content2);
		
		// update entry
		content.put("foo", "spam");
		String uid2 = update(endpoint, content);
		assertEquals(uid, uid2);
		entry = (RegistryEntryImpl)entryHome.get(uid);
		assertEquals(endpoint, entry.getModel().getEndpoint());
		registry = (RegistryImpl)home.get("default_registry");
		content2 = registry.getModel().getContent(endpoint);
		assertEquals("spam", content2.get("foo"));
		System.out.println("Entry for <"+entry.getModel().getEndpoint()+"> : "+content2);
		
	}

	private String update(String endpoint, Map<String,String>content) throws Exception {
		RegistryHome home = (RegistryHome)kernel.getHome("Registry");
		RegistryImpl registry = (RegistryImpl)home.getForUpdate("default_registry");
		registry.addEntry(endpoint, content, null);
		home.persist(registry);
		return registry.getModel().getEntryID(endpoint);
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
			home.activateHome(getName());
		}

		@Override
		public void stop() throws Exception {
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
			home.activateHome(getName());
		}

		@Override
		public void stop() throws Exception {
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
