package eu.unicore.services.rest.security.sshkey;

import java.io.File;
import java.security.PrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.Assert;
import org.junit.Test;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

public class TestUtils {

	@Test
	public void testRSAKey() throws Exception {
		File key = new File("src/test/resources/ssh/id_rsa");
		RSAPrivateKey pk = (RSAPrivateKey)SSHUtils.readPrivateKey(key, new Password("test123".toCharArray()));
		Assert.assertNotNull(pk);
		RSAPublicKey pub = (RSAPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_rsa.pub"));
		Assert.assertNotNull(pub);
	}

	@Test
	public void testDSAKey() throws Exception {
		File key = new File("src/test/resources/ssh/id_dsa");
		DSAPrivateKey pk = (DSAPrivateKey)SSHUtils.readPrivateKey(key, new Password("test123".toCharArray()));
		Assert.assertNotNull(pk);
		DSAPublicKey pub = (DSAPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_dsa.pub"));
		Assert.assertNotNull(pub);
	}

	@Test
	public void testECDSAKey() throws Exception {
		File key = new File("src/test/resources/ssh/id_ecdsa");
		PrivateKey pk = SSHUtils.readPrivateKey(key, new Password("test123".toCharArray()));
		Assert.assertNotNull(pk);
		ECPublicKey pub = (ECPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_ecdsa.pub"));
		Assert.assertNotNull(pub);
	}

	@Test
	public void testECDSAKey2() throws Exception {
		File key = new File("src/test/resources/ssh/id_ecdsa");
		PrivateKey pk = SSHUtils.readPrivateKey(key, new Password("test123".toCharArray()));
		Assert.assertNotNull(pk);
		ECPublicKey pub = (ECPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_ecdsa_384.pub"));
		Assert.assertNotNull(pub);
	}

	@Test
	public void testED25519() throws Exception {
		File key = new File("src/test/resources/ssh/id_ed25519");
		PrivateKey pk = SSHUtils.readPrivateKey(key, new Password("test123".toCharArray()));
		Assert.assertNotNull(pk);
		EdDSAPublicKey pub = (EdDSAPublicKey)SSHUtils.readPublicKey(new File("src/test/resources/ssh/id_ed25519.pub"));
		Assert.assertNotNull(pub);
	}
}
