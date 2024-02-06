package eu.unicore.services.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.unicore.persist.impl.InMemory;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.Kernel;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.security.TestConfigUtil;

public class TestPersistenceManager {

	@Test
	public void testLockSupport() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		LockSupport ls=k.getPersistenceManager().getLockSupport();
		assertNotNull(ls);
	}

	@Test
	public void testStoreHandling() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		PersistenceManager p=k.getPersistenceManager();
		Store s=p.getPersist("test");
		assertNotNull(s);
		Store s2=p.getPersist("test");
		assertNotNull(s2);
		assertTrue(s==s2);
		
		p.removePersist("test");
		s=p.persistMap.get("test");
		assertNull(s);
	}

	@Test
	public void testGetPersistenceSettings() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		PersistenceManager p=k.getPersistenceManager();
		PersistenceSettings ps=p.getPersistenceSettings(ResourceImpl.class);
		assertNotNull(ps);
	}

	@Test
	public void testInMemory() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		PersistenceManager p=k.getPersistenceManager();
		k.getPersistenceProperties().setProperty("class.test", InMemory.class.getName());
		Store s = p.getPersist("test");
		assertTrue(s instanceof Persistence);
		assertTrue (((Persistence)s).getBackEnd() instanceof InMemory);
		assertNotNull(s.getUniqueIDs());
	}

}
