package eu.unicore.services.rest.jwt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.interfaces.RSAPublicKey;
import java.util.Properties;
import java.util.Random;

import org.junit.jupiter.api.Test;

import eu.unicore.security.AuthenticationException;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.util.PubkeyCache;

public class TestJWTUtils {

	private ContainerSecurityProperties getContainerSecurityProperties(){
		Properties p = new Properties();
		p.put("container.security.credential.path", "src/test/resources/keystore.jks");
		p.put("container.security.credential.password", "the!njs");
		p.put("container.security.truststore.type", "directory");
		p.put("container.security.truststore.directoryLocations.1", "src/test/resources/cacert.pem");
		p.put("container.security.accesscontrol", "false");
		return new ContainerSecurityProperties(p);
	}
	
	@Test
	public void testHS256() throws Exception {
		ContainerSecurityProperties cp = getContainerSecurityProperties();
		Properties p = new Properties();
		byte[]keydata = new byte[256];
		new Random().nextBytes(keydata);
		String secret = new String(keydata);
		p.put("container.security.rest.jwt.hmacSecret", secret);
		JWTServerProperties jwtProps = new JWTServerProperties(p);
		JWTHelper jwt = new JWTHelper(jwtProps, cp, null);
		String token = jwt.createETDToken("CN=Some User,OU=Users");
		System.out.println("Token: "+token);
		System.out.println("Payload: "+JWTUtils.getPayload(token));
		System.out.println("Headers: "+JWTUtils.getHeaders(token));
		jwt.verifyJWTToken(token, null);
	}

	@Test
	public void testRS256() throws Exception {
		ContainerSecurityProperties cp = getContainerSecurityProperties();
		Properties p = new Properties();
		JWTServerProperties jwtProps = new JWTServerProperties(p);
		JWTHelper jwt = new JWTHelper(jwtProps, cp, getKeyCache(cp));
		String token = jwt.createETDToken("CN=Some User,OU=Users");
		System.out.println("Token: "+token);
		System.out.println("Payload: "+JWTUtils.getPayload(token));
		System.out.println("Headers: "+JWTUtils.getHeaders(token));
		jwt.verifyJWTToken(token, null);
	}
	
	@Test
	public void testFailOnExpiredToken() throws Exception {
		ContainerSecurityProperties cp = getContainerSecurityProperties();
		Properties p = new Properties();
		byte[]keydata = new byte[256];
		new Random().nextBytes(keydata);
		String secret = new String(keydata);
		p.put("container.security.rest.jwt.hmacSecret", secret);
		JWTServerProperties jwtProps = new JWTServerProperties(p);
		JWTHelper jwt = new JWTHelper(jwtProps, cp, new PubkeyCache());
		String token = jwt.createETDToken("CN=Some User,OU=Users", -72000);
		System.out.println("Token: "+token);
		System.out.println("Payload: "+JWTUtils.getPayload(token));
		System.out.println("Headers: "+JWTUtils.getHeaders(token));
		assertThrows(AuthenticationException.class, ()->{
			jwt.verifyJWTToken(token, null);
		});
	}

	private PubkeyCache getKeyCache(ContainerSecurityProperties cp){
		final RSAPublicKey key = (RSAPublicKey)cp.getCredential().getCertificate().getPublicKey();
		return new PubkeyCache() {
			@Override
			public RSAPublicKey getPublicKey(String subject) {
				return key;
			}
		};
	}

	@Test
	public void testLoadLocalTrustedIssuers() throws Exception {
		ContainerSecurityProperties cp = getContainerSecurityProperties();
		Properties p = new Properties();
		JWTServerProperties jwtProps = new JWTServerProperties(p);
		PubkeyCache pc = new PubkeyCache();
		new JWTHelper(jwtProps, cp, pc);

		p.put("container.security.rest.jwt."+JWTServerProperties.TRUSTED_ISSUER_CERT_LOCATIONS+"1",
				"no_such_file");
		jwtProps = new JWTServerProperties(p);
		pc = new PubkeyCache();
		new JWTHelper(jwtProps, cp, pc);

		p.put("container.security.rest.jwt."+JWTServerProperties.TRUSTED_ISSUER_CERT_LOCATIONS+"1",
				"src/test/resources/cacert.pem");
		jwtProps = new JWTServerProperties(p);
		pc = new PubkeyCache();
		new JWTHelper(jwtProps, cp, pc);
		assertNotNull(pc.getPublicKey("CN=Demo CA,O=UNICORE,C=EU"));
	}
}
