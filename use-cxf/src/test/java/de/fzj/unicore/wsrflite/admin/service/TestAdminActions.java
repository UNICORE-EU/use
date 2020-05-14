package de.fzj.unicore.wsrflite.admin.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;

public class TestAdminActions {

	@Test
	public void testAdminActionLoader() throws Exception{
		Kernel kernel=new Kernel(TestConfigUtil.getInsecureProperties());
		Map<String,AdminAction> act=kernel.getAdminActions();
		assertNotNull(act);
		AdminAction aAct=act.get("mock");
		assertNotNull(aAct);
		assertEquals("mock", aAct.getName());
		Map<String,String>params=new HashMap<String, String>();
		params.put("foo", "foo-value");
		AdminActionResult result=aAct.invoke(params, kernel);
		assertTrue(result.successful());
		assertEquals("ok", result.getMessage());
		assertEquals("echo-foo-value", result.getResults().get("foo"));
	}
	
}
