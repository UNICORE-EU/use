package eu.unicore.services.rest.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.security.TestConfigUtil;

public class TestAdminActions {

	@Test
	public void testAdminActions() throws Exception{
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

	@Test
	public void testIssueAPITokenAdminAction() throws Exception{
		Kernel kernel = new Kernel("src/test/resources/use.properties");
		Map<String,AdminAction> act = kernel.getAdminActions();
		assertNotNull(act);
		AdminAction aAct = act.get("IssueAPIToken");
		assertNotNull(aAct);
		assertEquals("IssueAPIToken", aAct.getName());
		Map<String,String>params = new HashMap<String, String>();
		params.put("subject", "CN=demouser");
		params.put("lifetime", "60");
		params.put("preferences", "uid:nobody");
		AdminActionResult result = aAct.invoke(params, kernel);
		assertTrue(result.successful());
		String token = result.getResults().get("token");
		JWTUtils.verifyJWTToken(token,
				kernel.getContainerSecurityConfiguration().getCredential().getCertificate().getPublicKey(),
				null);
		assertEquals("uid:nobody", JWTUtils.getPayload(token).get("preferences"));

		params = new HashMap<String, String>();
		params.put("subject", "CN=demouser");
		params.put("preferences", "invalid");
		AdminActionResult result2 = aAct.invoke(params, kernel);
		assertFalse(result2.successful());
		assertTrue(result2.getMessage().contains("IllegalArgumentException"));

		params = new HashMap<String, String>();
		params.put("subject", "CN=demouser");
		params.put("foo", "ham");
		params.put("bar", "spam");
		AdminActionResult result3 = aAct.invoke(params, kernel);
		assertFalse(result3.successful());
		assertTrue(result3.getMessage().contains("IllegalArgumentException"));
		assertTrue(result3.getMessage().contains("foo"));
	}

}
