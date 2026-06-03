package eu.unicore.services.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

public class TestAdminActions {

	static Kernel k;

	@BeforeAll
	static void start() throws Exception {
		k = new Kernel(TestConfigUtil.getInsecureProperties());
	}
	
	@AfterAll
	static void stop() throws Exception {
		k.shutdown();
	}

	@Test
	public void testAdminActionLoader() throws Exception{
		Map<String,AdminAction> act = k.getAdminActions();
		assertNotNull(act);
		for(String s: act.keySet()) {
			AdminAction a = act.get(s);
			System.out.println(a.getName()+" - "+a.getDescription());
		}
		AdminAction aAct=act.get("mock");
		assertNotNull(aAct);
		assertEquals("mock", aAct.getName());
		Map<String,String>params = new HashMap<>();
		params.put("foo", "foo-value");
		AdminActionResult result=aAct.invoke(params, k);
		assertTrue(result.successful());
		assertEquals("ok", result.getMessage());
		assertEquals("echo-foo-value", result.getResults().get("foo"));
	}

	@Test
	public void testResourceAvailabilityAction() throws Exception{
		Map<String,String>params = new HashMap<>();
		ResourceAvailability ra = (ResourceAvailability)k.getAdminActions().get("ToggleResourceAvailability");
		params.put("resources", "a,b,c");
		AdminActionResult res = ra.invoke(params, k);
		assertTrue(res.successful());
		assertTrue(ResourceAvailability.isUnavailable("a"));
		assertTrue(ResourceAvailability.isUnavailable("b"));
		assertTrue(ResourceAvailability.isUnavailable("c"));
		params.put("resources", "c");
		res = ra.invoke(params, k);
		assertTrue(res.successful());
		assertFalse(ResourceAvailability.isUnavailable("c"));
	}

	@Test
	public void testMemoryUsage() throws Exception{
		Map<String,String>params = new HashMap<>();
		GetMemoryUsage ra = (GetMemoryUsage)k.getAdminActions().get("GetMemoryUsage");
		AdminActionResult res = ra.invoke(params, k);
		assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
	}
}
