package eu.unicore.services.rest.security.jwt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

import eu.unicore.security.AuthenticationException;

public class JWTUtils {

	public static String getIssuer(String token) throws Exception {
		JWT jwt = JWTParser.parse(token);
		return jwt.getJWTClaimsSet().getIssuer();
	}

	public static JSONObject getPayload(String token) throws Exception {
		JWT jwt = JWTParser.parse(token);
		return new JSONObject(jwt.getJWTClaimsSet().toJSONObject().toString());
	}

	public static JSONObject getHeaders(String token) throws Exception {
		JWT jwt = JWTParser.parse(token);
		return new JSONObject(jwt.getHeader().toJSONObject().toJSONString());
	}

	final static Map<String,String>etd = new HashMap<>();
	static{
		etd.put("etd", "true");
	}
	
	public static String createETDToken(String user, long lifetime, String issuer, PrivateKey pk) throws Exception {
		return createJWTToken(user, lifetime, issuer, pk, etd);
	}
	
	public static String createJWTToken(String user, long lifetime, String issuer, PrivateKey pk, 
			Map<String,String> claims) throws Exception {
		JWTClaimsSet claimsSet = buildClaimsSet(user, lifetime, issuer, claims);
		JWSSigner signer = getSigner(pk);
		JWSAlgorithm alg = signer.supportedJWSAlgorithms().iterator().next();
		JWSHeader header = new JWSHeader(alg);
		SignedJWT sig = new SignedJWT(header, claimsSet);
		sig.sign(signer);
		return sig.serialize();
	}
	
	private static JWTClaimsSet buildClaimsSet(String user, long lifetime, String issuer, Map<String,String> claims) throws Exception {
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder(); 
		builder.issueTime(new Date(System.currentTimeMillis()))
				.subject(user)
				.issuer(issuer)
				.expirationTime(new Date(new Date().getTime() + lifetime * 1000));
		if(claims!=null){
			for(Map.Entry<String, String>claim: claims.entrySet()){
				builder.claim(claim.getKey(), claim.getValue());
			}
		}
		return builder.build();
	}
	
	public static JWSAlgorithm determineAlgorithm(PrivateKey pk){
		if(pk instanceof RSAPrivateKey){
			return JWSAlgorithm.RS256;
		}
		if(pk instanceof ECPrivateKey){
			return JWSAlgorithm.ES256;
		}
		throw new IllegalArgumentException("no algorithm for "+pk.getClass());
	}

	public static JWSSigner getSigner(PrivateKey pk) throws Exception {
		if(pk instanceof RSAPrivateKey){
			return new RSASSASigner(pk, true);
		}
		if(pk instanceof ECPrivateKey){
			return new ECDSASigner((ECPrivateKey)pk);
		}
		throw new IllegalArgumentException("no signer for "+pk.getClass());
	}
	
	public static JWSVerifier getVerifier(PublicKey pk) throws Exception {
		if(pk instanceof RSAPublicKey){
			return new RSASSAVerifier((RSAPublicKey)pk);
		}
		if(pk instanceof ECPublicKey){
			return new ECDSAVerifier((ECPublicKey)pk);
		}
		throw new IllegalArgumentException("no verifier for "+pk.getClass());
	}
	
	public static void verifyJWTToken(String token, PublicKey pk) throws Exception {
		try{
			SignedJWT sig = SignedJWT.parse(token);
			verifyClaims(sig.getJWTClaimsSet());
			if(!sig.verify(getVerifier(pk))){
				throw new BadJWTException("Signature verification failed!");
			}
		}catch(Exception ex){
			throw new AuthenticationException("JWT verification failed", ex);
		}
	}
	
	public static String createETDToken(String user, long lifetime, String issuer, String hmacSecret) throws Exception {
		return createJWTToken(user, lifetime, issuer, hmacSecret, etd);
	}
	
	public static String createJWTToken(String user, long lifetime, String issuer, String hmacSecret, Map<String,String>claims) throws Exception {
		JWTClaimsSet claimsSet = buildClaimsSet(user, lifetime, issuer, claims);
		JWSSigner signer = new MACSigner(hmacSecret);
		JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
		SignedJWT sig = new SignedJWT(header, claimsSet);
		sig.sign(signer);
		return sig.serialize();
	}
	
	public static void verifyJWTToken(String token, String hmacSecret) throws AuthenticationException {
		try{
			SignedJWT sig = SignedJWT.parse(token);
			verifyClaims(sig.getJWTClaimsSet());
			if(!sig.verify(new MACVerifier(hmacSecret))){
				throw new BadJWTException("Signature verification failed!");
			}
		}catch(Exception ex){
			throw new AuthenticationException("JWT verification failed", ex);
		}
	}
	
	public static void verifyClaims(JWTClaimsSet claims) throws BadJWTException {
		new DefaultJWTClaimsVerifier<SecurityContext>().verify(claims);
	}
}
