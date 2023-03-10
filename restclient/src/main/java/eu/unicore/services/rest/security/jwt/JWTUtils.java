package eu.unicore.services.rest.security.jwt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

import eu.unicore.security.AuthenticationException;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

public class JWTUtils {

	public static String getIssuer(String token) throws Exception {
		JWT jwt = JWTParser.parse(token);
		return jwt.getJWTClaimsSet().getIssuer();
	}

	public static JSONObject getPayload(String token) throws Exception {
		JWT jwt = JWTParser.parse(token);
		JSONObject j = new JSONObject();
		for(Map.Entry<String, Object>e: jwt.getJWTClaimsSet().toJSONObject().entrySet()) {
			j.put(e.getKey(), e.getValue());
		}
		return j;
	}

	public static JSONObject getHeaders(String token) throws Exception {
		JWT jwt = JWTParser.parse(token);
		JSONObject j = new JSONObject();
		for(Map.Entry<String, Object>e: jwt.getHeader().toJSONObject().entrySet()) {
			j.put(e.getKey(), e.getValue());
		}
		return j;
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
	
	public static JWTClaimsSet buildClaimsSet(String user, long lifetime, String issuer, Map<String,String> claims) throws Exception {
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder(); 
		builder.issueTime(new Date(System.currentTimeMillis()))
				.subject(user)
				.issuer(issuer)
				.expirationTime(new Date(System.currentTimeMillis() + (lifetime * 1000)));
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
		else if(pk instanceof ECPrivateKey){
			return JWSAlgorithm.ES256;
		}
		else if (pk instanceof EdDSAPrivateKey) {
			return JWSAlgorithm.EdDSA;
		}
		throw new IllegalArgumentException("no algorithm for "+pk.getClass());
	}

	public static JWSSigner getSigner(PrivateKey pk) throws Exception {
		JWSSigner signer = null;
		if(pk instanceof RSAPrivateKey){
			signer = new RSASSASigner(pk);
		}
		else if(pk instanceof ECPrivateKey){
			signer = new ECDSASigner((ECPrivateKey)pk);
		}
		else if(pk instanceof EdDSAPrivateKey) {
			EdDSAPrivateKey epk = (EdDSAPrivateKey)pk;
			OctetKeyPair key = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(epk.getAbyte()))
					.d(Base64URL.encode(epk.getSeed())).algorithm(JWSAlgorithm.EdDSA)
					.keyID("1").build();
			signer = new Ed25519Signer(key);
		}
		if(signer==null)throw new IllegalArgumentException("no signer for "+pk.getClass());
		signer.getJCAContext().setProvider(BouncyCastleProviderSingleton.getInstance());
		return signer;
	}
	
	public static JWSVerifier getVerifier(PublicKey pk) throws Exception {
		JWSVerifier verifier = null;
		if(pk instanceof RSAPublicKey){
			verifier = new RSASSAVerifier((RSAPublicKey)pk);
		}
		else if(pk instanceof ECPublicKey){
			verifier = new ECDSAVerifier((ECPublicKey)pk);
		}
		else if(pk instanceof EdDSAPublicKey){
			EdDSAPublicKey edKey = (EdDSAPublicKey)pk;
			OctetKeyPair key = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(edKey.getAbyte()))
					.d(null).algorithm(JWSAlgorithm.EdDSA).keyID("1").build();
			verifier = new Ed25519Verifier(key);
		}
		if(verifier==null)throw new IllegalArgumentException("no verifier for "+pk.getClass());
		verifier.getJCAContext().setProvider(BouncyCastleProviderSingleton.getInstance());
		return verifier;
	}

	public static void verifyJWTToken(String token, PublicKey pk, String allowedAudience) throws AuthenticationException {
		try{
			SignedJWT sig = SignedJWT.parse(token);
			verifyClaims(sig.getJWTClaimsSet(), allowedAudience);
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
	
	public static void verifyJWTToken(String token, String hmacSecret, String allowedAudience) throws AuthenticationException {
		try{
			SignedJWT sig = SignedJWT.parse(token);
			verifyClaims(sig.getJWTClaimsSet(), allowedAudience);
			if(!sig.verify(new MACVerifier(hmacSecret))){
				throw new BadJWTException("Signature verification failed!");
			}
		}catch(Exception ex){
			throw new AuthenticationException("JWT verification failed", ex);
		}
	}
	
	public static boolean isHMAC(String token) throws AuthenticationException {
		try {
			JSONObject headers = JWTUtils.getHeaders(token);
			return headers.getString("alg").startsWith("HS");
		}catch(Exception e) {
			throw new AuthenticationException("Invalid token", e);
		}
	}
	
	static void verifyClaims(JWTClaimsSet claims, String serverDN) throws BadJWTException {
		Set<String>requiredClaims = new HashSet<>();
		requiredClaims.add("exp");
		// only check audience if it is in the JWT
		String requiredAudience = null;
		if(!claims.getAudience().isEmpty()) {
			requiredAudience = serverDN;
		}
		new DefaultJWTClaimsVerifier<SecurityContext>(requiredAudience, null, requiredClaims)
			.verify(claims, null);
	}
	
}
