package eu.unicore.services.rest.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.security.TestConfigUtil;

public class TestAdminActions {

	@BeforeEach
	public void clean() {
		FileUtils.deleteQuietly(new File("./target","data"));
	}

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
		AdminActionResult result = issue("CN=demouser", "60", "uid:nobody", kernel);
		assertTrue(result.successful(), result.getMessage());
		String token = result.getResults().get("token");
		String tokenID = result.getResults().get("tokenID");
		JWTUtils.verifyJWTToken(token,
				kernel.getContainerSecurityConfiguration().getCredential().getCertificate().getPublicKey(),
				null);
		assertEquals("uid:nobody", JWTUtils.getPayload(token).get("preferences"));
		assertEquals("CN=demouser", JWTUtils.getPayload(token).get("sub"));
		System.out.println(JWTUtils.getPayload(token).toString(2));
		String jti = JWTUtils.getPayload(token).getString("jti");
		assertNotNull(jti);
		assertEquals(tokenID, jti);

		AdminActionResult result2 = issue("CN=demouser", null, "invalid", kernel);
		assertFalse(result2.successful());
		assertTrue(result2.getMessage().contains("IllegalArgumentException"));

		Map<String, String> params = new HashMap<>();
		params.put("subject", "CN=demouser");
		params.put("foo", "ham");
		params.put("bar", "spam");
		AdminActionResult result3 = kernel.getAdminActions().get("IssueAPIToken").invoke(params, kernel);
		assertFalse(result3.successful());
		assertTrue(result3.getMessage().contains("IllegalArgumentException"));
		assertTrue(result3.getMessage().contains("foo"));
	}

	@Test
	public void testListAPITokens() throws Exception{
		Kernel kernel = new Kernel("src/test/resources/use.properties");
		AdminActionResult result = issue("CN=demouser", "60", "uid:nobody", kernel);
		assertTrue(result.successful(), result.getMessage());
		String tokenID = result.getResults().get("tokenID");
		issue("CN=demouser2", "60", "uid:nobody", kernel);
		issue("CN=other", "60", "uid:nobody", kernel);
		AdminAction list = kernel.getAdminActions().get("ListAPITokens");
		assertNotNull(list);
		// list all
		var params = new HashMap<String, String>();
		AdminActionResult listResult = list.invoke(params, kernel);
		assertTrue(listResult.successful(), result.getMessage());
		assertEquals(3, listResult.getResults().size());
		assertTrue(listResult.getResults().keySet().contains(tokenID));
		for(String tID : listResult.getResults().keySet()) {
			System.out.println("Token "+tID+": "+listResult.getResults().get(tID));
		}
		// list by matching subjects
		params = new HashMap<String, String>();
		params.put("subjectsMatch", "demo");
		listResult = list.invoke(params, kernel);
		assertTrue(listResult.successful(), result.getMessage());
		assertEquals(2, listResult.getResults().size());
	}

	@Test
	public void testRevokeAPITokens() throws Exception{
		Kernel kernel = new Kernel("src/test/resources/use.properties");
		AdminActionResult result = issue("CN=revoke1", "60", null, kernel);
		String token = result.getResults().get("token");
		String jti = JWTUtils.getPayload(token).getString("jti");
		assertNotNull(jti);

		AdminAction revokeAction = kernel.getAdminActions().get("RevokeAPITokens");
		assertNotNull(revokeAction);
		
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
		issue("CN=revoke2", null, null, kernel);
		params.clear();
		params.put("subject", "CN=revoke2");
		result4 = revokeAction.invoke(params, kernel);
		assertTrue(result4.getMessage().contains("<1> token(s) revoked"), result4.getMessage());
		assertTrue(result4.successful());

		issue("CN=foo1", null, null, kernel);
		issue("CN=foo1", null, null, kernel);
		issue("CN=foo2", null, null, kernel);

		// delete all matching subject
		params.clear();
		params.put("subjectsMatch", "foo");
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

	
	private AdminActionResult issue(String subject, String lifetime, String preferences, Kernel k) {
		AdminAction aAct = k.getAdminActions().get("IssueAPIToken");
		assertNotNull(aAct);
		assertEquals("IssueAPIToken", aAct.getName());
		Map<String,String>params = new HashMap<String, String>();
		if(subject!=null)params.put("subject", subject);
		if(lifetime!=null)params.put("lifetime", lifetime);
		if(preferences!=null)params.put("preferences", preferences);
		return aAct.invoke(params, k);
	}

}
