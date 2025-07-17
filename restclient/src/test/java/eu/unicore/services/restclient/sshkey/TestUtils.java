package eu.unicore.services.restclient.sshkey;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jcajce.interfaces.EdDSAPrivateKey;
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;
import org.junit.jupiter.api.Test;

public class TestUtils {

	@Test
	public void testRSAKey() throws Exception {
		File key = new File("src/test/resources/ssh/id_rsa");
		RSAPrivateKey pk = (RSAPrivateKey)SSHUtils.readPrivateKey(key, new PasswordSupplierImpl("test123".toCharArray()));
		assertNotNull(pk);
		RSAPublicKey pub = (RSAPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_rsa.pub"));
		assertNotNull(pub);
	}

	@Test
	public void testDSAKey() throws Exception {
		File key = new File("src/test/resources/ssh/id_dsa");
		DSAPrivateKey pk = (DSAPrivateKey)SSHUtils.readPrivateKey(key, new PasswordSupplierImpl("test123".toCharArray()));
		assertNotNull(pk);
		DSAPublicKey pub = (DSAPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_dsa.pub"));
		assertNotNull(pub);
	}

	@Test
	public void testECDSAKey() throws Exception {
		File key = new File("src/test/resources/ssh/id_ecdsa");
		PrivateKey pk = SSHUtils.readPrivateKey(key, new PasswordSupplierImpl("test123".toCharArray()));
		assertNotNull(pk);
		ECPublicKey pub = (ECPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_ecdsa.pub"));
		assertNotNull(pub);
	}

	@Test
	public void testECDSAKey2() throws Exception {
		File key = new File("src/test/resources/ssh/id_ecdsa");
		PrivateKey pk = SSHUtils.readPrivateKey(key, new PasswordSupplierImpl("test123".toCharArray()));
		assertNotNull(pk);
		ECPublicKey pub = (ECPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_ecdsa_384.pub"));
		assertNotNull(pub);
	}

	@Test
	public void testED25519() throws Exception {
		File key = new File("src/test/resources/ssh/id_ed25519");
		EdDSAPrivateKey pk = (EdDSAPrivateKey )SSHUtils.readPrivateKey(key, new PasswordSupplierImpl("test123".toCharArray()));
		assertNotNull(pk);
		EdDSAPublicKey pub = (EdDSAPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_ed25519.pub"));
		assertNotNull(pub);
	}
	
	@Test
	public void testPuttyKey() throws Exception {
		File key = new File("src/test/resources/ssh/putty-key");
		PrivateKey pk = SSHUtils.readPrivateKey(key, new PasswordSupplierImpl((char[])null));
		assertNotNull(pk);
		PublicKey pub = SSHUtils.readPublicKey(new File("src/test/resources/ssh/putty-key.pub"));
		assertNotNull(pub);
	}

	@Test
	public void testLegacyAuth() throws Exception {
		File key = new File("src/test/resources/ssh/id_ed25519");
		File pub = new File("src/test/resources/ssh/id_ed25519.pub");
		SSHKeyUC auth = SSHUtils.createAuthData(key, "test123".toCharArray(), "thisisatesttoken");
		assertTrue(SSHUtils.validateAuthData(auth, FileUtils.readFileToString(pub, "UTF-8")));
	}
}
