package eu.unicore.services.aip.xuudb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.SubjectAttributesHolder;

public class TestCredentialCache {

	public void test1() throws Exception {
		CredentialCache c = new CredentialCache(1);
		Object o1 = new Object();
		SubjectAttributesHolder holder = new SubjectAttributesHolder();
		Map<String,String[]>m1 = new HashMap<>();
		holder.setAllIncarnationAttributes(m1, m1);
		c.put(o1, holder);
		assertNotNull(c.read(o1));
		assertEquals(holder, c.read(o1));
		c.removeAll();
		assertNull(c.read(o1));
		c.put(o1, holder);
		//check time to live, is 1 sec by default...
		Thread.sleep(2000);
		assertNull(c.read(o1));
		CredentialCache c2=new CredentialCache(1);
		assertNotNull(c2);
	}
}
