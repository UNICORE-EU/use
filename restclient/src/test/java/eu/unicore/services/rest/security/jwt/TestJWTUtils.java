package eu.unicore.services.rest.security.jwt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;

import eu.unicore.security.AuthenticationException;
import eu.unicore.services.rest.security.sshkey.PasswordSupplierImpl;
import eu.unicore.services.rest.security.sshkey.SSHKey;
import eu.unicore.services.rest.security.sshkey.SSHUtils;

public class TestJWTUtils {

	@Test
	public void testHMAC() throws Exception {
		String key = new String("12345678901234567890123456789012");
		String token = JWTUtils.createJWTToken("demouser", 72000, "demouser", key, null);
		assertTrue(JWTUtils.isHMAC(token));
		JSONObject payload = JWTUtils.getPayload(token);
		assertEquals("demouser", payload.getString("sub"));
		assertEquals("demouser", payload.getString("iss"));
		JSONObject headers = JWTUtils.getHeaders(token);
		System.out.println(headers);
		System.out.println(payload);
		JWTUtils.verifyJWTToken(token, key, null);
	}
	
	@Test
	public void testAudience1() throws Exception {
		String key = new String("12345678901234567890123456789012");
		String audience = "CN=my server";
		var claims = new HashMap<String,String>();
		claims.put("aud", audience);
		String token = JWTUtils.createJWTToken("demouser", 72000, "demouser", key, claims);
		
		assertTrue(JWTUtils.isHMAC(token));
		JSONObject payload = JWTUtils.getPayload(token);
		assertEquals("demouser", payload.getString("sub"));
		assertEquals("demouser", payload.getString("iss"));
		assertEquals(audience, payload.getString("aud"));
		
		JSONObject headers = JWTUtils.getHeaders(token);
		System.out.println(headers);
		System.out.println(payload);
		JWTUtils.verifyJWTToken(token, key, audience);
	}
	
	@Test
	public void testAudienceFailsValidation() throws Exception {
		String key = new String("12345678901234567890123456789012");
		String audience = "CN=my server";
		var claims = new HashMap<String,String>();
		claims.put("aud", audience);
		String token = JWTUtils.createJWTToken("demouser", 72000, "demouser", key, claims);

		assertTrue(JWTUtils.isHMAC(token));
		JSONObject payload = JWTUtils.getPayload(token);
		assertEquals("demouser", payload.getString("sub"));
		assertEquals("demouser", payload.getString("iss"));
		assertEquals(audience, payload.getString("aud"));

		JSONObject headers = JWTUtils.getHeaders(token);
		System.out.println(headers);
		System.out.println(payload);

		assertThrows(AuthenticationException.class, ()->{
			JWTUtils.verifyJWTToken(token, key, "CN=my other server");
		});
	}

	@Test
	public void testFailOnExpiredToken() throws Exception {
		byte[] secret = new byte[384];
		new Random().nextBytes(secret);
		String key = new String(secret);
		String token = JWTUtils.createJWTToken("demouser", -7200, "demouser", key, null);

		JSONObject payload = JWTUtils.getPayload(token);
		assertEquals("demouser", payload.getString("sub"));
		assertEquals("demouser", payload.getString("iss"));
		JSONObject headers = JWTUtils.getHeaders(token);
		System.out.println(headers);
		System.out.println(payload);

		assertThrows(AuthenticationException.class, ()->{
			JWTUtils.verifyJWTToken(token, key, null);
		});
	}

	@Test
	public void testKeybasedJWT() throws Exception {
		String[] keys = {"id_rsa", "id_ecdsa", "id_ecdsa_384", "id_ed25519", "putty-key"};
		for(String k : keys){
			File key = new File("src/test/resources/ssh/"+k);
			SSHKey sk = new SSHKey("demouser", key, new PasswordSupplierImpl("test123".toCharArray()), 300);
			String token = sk.getToken();
			assertFalse(JWTUtils.isHMAC(token));
			JSONObject payload = JWTUtils.getPayload(token);
			assertEquals("demouser", payload.getString("sub"));
			assertEquals("demouser", payload.getString("iss"));
			JSONObject headers = JWTUtils.getHeaders(token);
			if(k.contains("rsa")){
				assertTrue(headers.getString("alg").startsWith("RS"));
			}
			if(k.contains("dsa")){
				assertTrue(headers.getString("alg").startsWith("ES"));
			}
			// verify
			PublicKey pub = SSHUtils.readPublicKey(new File("src/test/resources/ssh/"+k+".pub"));
			JWTUtils.verifyJWTToken(token, pub, null);
		}
	}

	@Test
	public void testFailOnMissingClaims() throws Exception{
		JWTClaimsSet c = new JWTClaimsSet.Builder()
				.issueTime(new Date(System.currentTimeMillis()))
				.subject("CN=subject")
				.issuer("CN=issuer")
				.build();
		assertThrows(BadJWTException.class, ()->{
			JWTUtils.verifyClaims(c, null);
		});
	}

}
