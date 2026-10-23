package eu.unicore.services.rest.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
	public void testIssueAPIToken() throws Exception{
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
		assertTrue(result.successful(), result.getMessage());
		String token = result.getResults().get("token");
		JWTUtils.verifyJWTToken(token,
				kernel.getContainerSecurityConfiguration().getCredential().getCertificate().getPublicKey(),
				null);
		assertEquals("uid:nobody", JWTUtils.getPayload(token).get("preferences"));
		assertEquals("CN=demouser", JWTUtils.getPayload(token).get("sub"));
		System.out.println(JWTUtils.getPayload(token).toString(2));
		String jti = JWTUtils.getPayload(token).getString("jti");
		assertNotNull(jti);
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

	@Test
	public void testRevokeAPIToken() throws Exception{
		Kernel kernel = new Kernel("src/test/resources/use.properties");
		Map<String,AdminAction> act = kernel.getAdminActions();
		assertNotNull(act);
		AdminAction createAction = act.get("IssueAPIToken");
		Map<String,String>cparams = new HashMap<>();
		cparams.put("subject", "CN=revoke1");
		cparams.put("lifetime", "60");
		AdminActionResult result = createAction.invoke(cparams, kernel);
		String token = result.getResults().get("token");
		String jti = JWTUtils.getPayload(token).getString("jti");
		assertNotNull(jti);

		AdminAction revokeAction = act.get("RevokeAPIToken");
		assertNotNull(revokeAction);
		assertEquals("RevokeAPIToken", revokeAction.getName());
		
		Map<String,String> params = new HashMap<>();
		assertFalse(revokeAction.invoke(params, kernel).successful());

		params.put("subject", "test");
		params.put("tokenID", "foo");
		result = revokeAction.invoke(params, kernel);
		assertFalse(result.successful());
		assertTrue(result.getMessage().contains("Only one"));
		
		params.put("subject", "test");
		params.put("wrongParam", "foo");
		result = revokeAction.invoke(params, kernel);
		assertFalse(result.successful());
		assertTrue(result.getMessage().contains("Unknown parameter"));
		assertTrue(result.getMessage().contains("wrongParam"));

		// delete by ID
		params = new HashMap<>();
		params.put("tokenID", jti);
		AdminActionResult result4 = revokeAction.invoke(params, kernel);
		assertTrue(result4.successful());
		assertTrue(result4.getMessage().contains("<1> token(s) revoked"), result4.getMessage());

		// delete by subject
		cparams.put("subject", "CN=revoke2");
		createAction.invoke(cparams, kernel);
		params.clear();
		params.put("subject", "CN=revoke2");
		result4 = revokeAction.invoke(params, kernel);
		assertTrue(result4.getMessage().contains("<1> token(s) revoked"), result4.getMessage());
		assertTrue(result4.successful());

		cparams.put("subject", "CN=foo1");
		createAction.invoke(cparams, kernel);
		cparams.put("subject", "CN=foo1");
		createAction.invoke(cparams, kernel);
		cparams.put("subject", "CN=foo2");
		createAction.invoke(cparams, kernel);
		// delete by subject regexp
		params.clear();
		params.put("subjectsMatch", ".*foo.*");
		result4 = revokeAction.invoke(params, kernel);
		assertTrue(result4.successful());	
		assertTrue(result4.getMessage().contains("<3> token(s) revoked"), result4.getMessage());
	}

	@Test
	public void testSetPasswordAdminAction() throws Exception{
		FileUtils.touch(new File("target","test-userauthfile.txt"));
		Kernel kernel = new Kernel("src/test/resources/use.properties");
		Map<String,AdminAction> act = kernel.getAdminActions();
		assertNotNull(act);
		AdminAction aAct = act.get("SetPassword");
		assertNotNull(aAct);
		assertEquals("SetPassword", aAct.getName());
		Map<String,String>params = new HashMap<>();
		assertTrue(aAct.invoke(params, kernel).getMessage().contains("'username' is required"));
		params.put("username", "demouser2");
		assertTrue(aAct.invoke(params, kernel).getMessage().contains("'password' is required"));
		params.put("username", "demouser2");
		params.put("password", "foo");
		AdminActionResult result = aAct.invoke(params, kernel);
		assertFalse(result.successful());
		assertTrue(result.getMessage().contains("no such user"));
		params.put("username", "demouser2");
		params.put("password", "foo");
		params.put("dn", "CN=test123");
		result = aAct.invoke(params, kernel);
		assertTrue(result.successful());
	}

}
