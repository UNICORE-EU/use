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
	
	@Test
	public void testCheckToken() throws Exception {
		String tok = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJfNkZVSHFaSDNIRmVhS0pEZDhXcU"
				+ "x6LWFlZ3kzYXFodVNJZ1RXaTA1U2k0In0.eyJleHAiOjE2NDk3NTYxMTEsImlhdCI6MTY0OTE1MTMxMSw"
				+ "ianRpIjoiNGU3ODk0M2MtMTZhMi00MDQzLWIyMzktNGY4ZTBiOTUxYWM1IiwiaXNzIjoiaHR0cHM6Ly9pY"
				+ "W0uaHVtYW5icmFpbnByb2plY3QuZXUvYXV0aC9yZWFsbXMvaGJwIiwiYXVkIjoicmVhbG0tbWFuYWdlbWV"
				+ "udCIsInN1YiI6ImIxZDQ4ZmMwLTA1YTAtNGI1My05ZWJhLTI5ZDE0OGM4M2VhZiIsInR5cCI6IkJlYXJlc"
				+ "iIsImF6cCI6ImRldmVsb3BlciIsInNlc3Npb25fc3RhdGUiOiI0YmRkNDk4Ni0zZmMzLTRmZDgtYmVlYS0"
				+ "wNDExZTEyOTA2MTciLCJhY3IiOiIxIiwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiO"
				+ "nsicm9sZXMiOlsiY3JlYXRlLWNsaWVudCJdfX0sInNjb3BlIjoiIiwic2lkIjoiNGJkZDQ5ODYtM2ZjMy0"
				+ "0ZmQ4LWJlZWEtMDQxMWUxMjkwNjE3In0.zFvIiy4XgzJsi3Wb2-Rnw0CzU8ua6koOYCAlMP8ZoWD4ot-8q"
				+ "0Zh_bCqaU3NgudfyClDs2rl0VZnl6j-XrN24KyeE2_Ud6FASDbZSUWQAo1a_m12JjQGFvVLjuEhKIawkNd"
				+ "IkfdGuRw-d1C94m7YIoKgFOROpCNaoH8cFY81RtEWoIcE2Oz5Msbjx28XY3_DdOkj4GTrGZXbYFXf8Gh1d"
				+ "2NwOeyEiOcH4tHPb-Eq6bu2kB0acnZ0TQgVLHSPWawhEmbQYte-n6bDeJ0Kp-mrAcNGrsutLC6GZUo4QPD"
				+ "XvTHeK6PzvYGl4uk6zBiSWqK5RMXS1UiuQhD5rFi1bG5_Tg";
	System.out.println(JWTUtils.getPayload(tok).toString(2));
	System.out.println(JWTUtils.getHeaders(tok).toString(2));
	
	}
}
