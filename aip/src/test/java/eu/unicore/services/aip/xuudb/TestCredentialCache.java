package eu.unicore.services.aip.xuudb;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.SubjectAttributesHolder;
import junit.framework.TestCase;

public class TestCredentialCache extends TestCase{

	public void test1(){
		try{
			CredentialCache c=new CredentialCache(1);
			Object o1=new Object();
			SubjectAttributesHolder holder = new SubjectAttributesHolder();
			Map<String,String[]>m1=new HashMap<String, String[]>();
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
		}catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
