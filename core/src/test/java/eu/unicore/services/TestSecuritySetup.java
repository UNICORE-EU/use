package eu.unicore.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.services.security.CertificateInfoMetric;

public class TestSecuritySetup
{
	private String p1 = "container.security.truststore.keystorePath=src/test/resources/conf/truststore.jks\n" +
			"container.security.truststore.keystorePassword=unicore\n" +
			"container.security.truststore.type=keystore\n" +
			"container.security.credential.format=pkcs12\n" +
			"container.security.credential.path=src/test/resources/conf/unicorex.p12\n" +
			"container.security.credential.password=the!njs\n" +
			"container.security.credential.keyAlias=unicorex\n" +
			"container.security.accesscontrol=false\n" +
			"container.security.gateway.enable=false\n" +
		    "persistence.directory=/tmp";

	private String p2 = "container.security.truststore.keystorePath=src/test/resources/conf/truststore.jks\n" +
			"container.security.truststore.keystorePassword=\n" +
			"container.security.truststore.type=keystore\n" +
			"container.security.credential.format=pkcs12\n" +
			"container.security.credential.path=src/test/resources/conf/unicorex.p12\n" +
			"container.security.credential.password=the!njs\n" +
			"container.security.credential.keyAlias=unicorex\n" +
			"container.security.accesscontrol=false\n" +
			"container.security.gateway.enable=false\n" +
			"persistence.directory=/tmp";

	private String p3 = "container.security.truststore.keystorePath=src/test/resources/conf/truststore.jks\n" +
			"container.security.truststore.keystorePassword=unicore\n" +
			"container.security.truststore.type=keystore\n" +
			"container.security.credential.format=pkcs12\n" +
			"container.security.credential.path=src/test/resources/conf/unicorex.p12\n" +
			"container.security.credential.password=\n" +
			"container.security.credential.keyAlias=unicorex\n" +
			"container.security.accesscontrol=false\n" +
			"container.security.gateway.enable=false\n" +
			"persistence.directory=/tmp";

	@Test
	public void testValidSettings() throws Exception {
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(p1.getBytes()));
		new Kernel(p);
	}
		
	@Test
	public void testInvalidSettings() throws Exception {
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(p2.getBytes()));
		assertThrows(Exception.class, ()->{
			new Kernel(p);
		});
	}
	
	@Test
	public void testInvalidSettings2() throws Exception {
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(p3.getBytes()));
		assertThrows(Exception.class, ()->{
			new Kernel(p);
		});
	}

	@Test
	public void testCertInfoMetric() throws Exception{
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(p1.getBytes()));
		Kernel k = new Kernel(p);
		String certInfo = new CertificateInfoMetric(k.getSecurityManager()).getValue();
		assertTrue(certInfo.contains("ServerIdentity"));
		assertTrue(certInfo.contains("Expires"));
		assertTrue(certInfo.contains("IssuedBy"));
	}

}
