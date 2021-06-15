package eu.unicore.services.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

public class TestAdminActions {

	@Test
	public void testAdminActionLoader() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		Map<String,AdminAction> act=k.getAdminActions();
		assertNotNull(act);
		AdminAction aAct=act.get("mock");
		assertNotNull(aAct);
		assertEquals("mock", aAct.getName());
		Map<String,String>params=new HashMap<String, String>();
		params.put("foo", "foo-value");
		AdminActionResult result=aAct.invoke(params, k);
		assertTrue(result.successful());
		assertEquals("ok", result.getMessage());
		assertEquals("echo-foo-value", result.getResults().get("foo"));
	}
	
}
