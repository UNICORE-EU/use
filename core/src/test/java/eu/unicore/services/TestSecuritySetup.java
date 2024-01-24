package eu.unicore.services;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import eu.unicore.services.security.CertificateInfoMetric;
import eu.unicore.util.Log;
import junit.framework.TestCase;

public class TestSecuritySetup extends TestCase
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

	public void testInvalidSettings()
	{
		try
		{
			Properties p = new Properties();
			p.load(new ByteArrayInputStream(p1.getBytes()));
			new Kernel(p);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Should get no error");
		}

		try
		{
			Properties p = new Properties();
			p.load(new ByteArrayInputStream(p2.getBytes()));
			new Kernel(p);
			fail("Should get an error");
		} catch (Exception e)
		{
			//OK
			System.out.println("Got an expected error: "+Log.createFaultMessage("",e));
		}

		try
		{
			Properties p = new Properties();
			p.load(new ByteArrayInputStream(p3.getBytes()));
			new Kernel(p);
			fail("Should get an error");
		} catch (Exception e)
		{
			//OK
			System.out.println("Got an expected error: "+Log.createFaultMessage("",e));
		}
	}

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
