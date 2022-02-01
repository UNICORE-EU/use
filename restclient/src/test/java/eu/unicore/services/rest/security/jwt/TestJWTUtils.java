package eu.unicore.services.rest.security.jwt;

import java.io.File;
import java.security.PublicKey;
import java.util.Date;
import java.util.Random;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;

import eu.unicore.security.AuthenticationException;
import eu.unicore.services.rest.security.sshkey.Password;
import eu.unicore.services.rest.security.sshkey.SSHKey;
import eu.unicore.services.rest.security.sshkey.SSHUtils;

public class TestJWTUtils {

	@Test
	public void testHMAC() throws Exception {
		String key = new String("12345678901234567890123456789012");
		String token = JWTUtils.createJWTToken("demouser", 72000, "demouser", key, null);
		
		JSONObject payload = JWTUtils.getPayload(token);
		Assert.assertEquals("demouser", payload.getString("sub"));
		Assert.assertEquals("demouser", payload.getString("iss"));
		JSONObject headers = JWTUtils.getHeaders(token);
		System.out.println(headers);
		System.out.println(payload);
		
		// verify
		JWTUtils.verifyJWTToken(token, key);
	}
	
	@Test(expected=AuthenticationException.class)
	public void testFailOnExpiredToken() throws Exception {
		byte[] secret = new byte[384];
		new Random().nextBytes(secret);
		String key = new String(secret);
		String token = JWTUtils.createJWTToken("demouser", -7200, "demouser", key, null);
		
		JSONObject payload = JWTUtils.getPayload(token);
		Assert.assertEquals("demouser", payload.getString("sub"));
		Assert.assertEquals("demouser", payload.getString("iss"));
		JSONObject headers = JWTUtils.getHeaders(token);
		System.out.println(headers);
		System.out.println(payload);
		
		// verify
		JWTUtils.verifyJWTToken(token, key);
	}

	@Test
	public void testKeybasedJWT() throws Exception {
		String[] keys = {"id_rsa", "id_ecdsa", "id_ecdsa_384"};
		for(String k : keys){
			File key = new File("src/test/resources/ssh/"+k);
			SSHKey sk = new SSHKey("demouser", key, new Password("test123".toCharArray()), 300);
			String token = sk.getToken();
			JSONObject payload = JWTUtils.getPayload(token);
			Assert.assertEquals("demouser", payload.getString("sub"));
			Assert.assertEquals("demouser", payload.getString("iss"));
			JSONObject headers = JWTUtils.getHeaders(token);
			if(k.contains("rsa")){
				Assert.assertTrue(headers.getString("alg").startsWith("RS"));
			}
			if(k.contains("dsa")){
				Assert.assertTrue(headers.getString("alg").startsWith("ES"));
			}
			// verify
			PublicKey pub = SSHUtils.readPublicKey(new File("src/test/resources/ssh/"+k+".pub"));
			JWTUtils.verifyJWTToken(token, pub);
		}
	}

	@Test(expected=BadJWTException.class)
	public void testFailOnMissingClaims() throws Exception{
		JWTClaimsSet c = new JWTClaimsSet.Builder()
				.issueTime(new Date(System.currentTimeMillis()))
				.subject("CN=subject")
				.issuer("CN=issuer")
				.build();
		JWTUtils.verifyClaims(c);
	}
}
