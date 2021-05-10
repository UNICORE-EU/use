/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.uas.security.integration;

import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PREFIX;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_AIP_COMBINING_POLICY;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_AIP_ORDER;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_AIP_PREFIX;

import java.util.Properties;

import junit.framework.TestCase;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.ContainerSecurityProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.uas.security.ForwardService;
import eu.unicore.uas.security.file.FileAttributeSource;
import eu.unicore.uas.security.ldap.LDAPAttributeSource;
import eu.unicore.uas.security.vo.SAMLPullAuthoriser;
import eu.unicore.uas.security.xuudb.XUUDBAuthoriser;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;

public class TestCombiningAlgorithms extends TestCase
{
	protected Kernel kernel;

	protected Properties getProperties()
	{
		Properties ret = new Properties();
		ret.setProperty(PREFIX+ContainerSecurityProperties.PROP_CHECKACCESS_PDP, MockPDP.class.getName());
		
		ret.setProperty(PREFIX+PROP_AIP_ORDER, "LDAP VO-PULL XUUDB FILE");
		//ret.setProperty(PREFIX+PROP_AIP_ORDER, "FILE");
		ret.setProperty(PREFIX+PROP_AIP_COMBINING_POLICY, "FIRST_ACCESSIBLE");

		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.class", LDAPAttributeSource.class.getName());
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapPort", "12345");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapHost", "ldap://localhost");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapRootDn", "DC=foo");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapAuthentication", "simple");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapPrincipal", "fooUser");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapCredential", "bar");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".LDAP.ldapMaxConnectionsRetry", "1");

		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".VO-PULL.class", SAMLPullAuthoriser.class.getName());
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".VO-PULL.configurationFile", "src/test/resources/vo-pull.config");
		
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".XUUDB.class", XUUDBAuthoriser.class.getName());
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".XUUDB.xuudbHost", "http://localhost");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".XUUDB.xuudbPort", "62998");
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".XUUDB.xuudbGCID", "DUMMY");

		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".FILE.class", FileAttributeSource.class.getName());
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".FILE.file", "src/test/resources/file/testUudb-demouser.xml");
		
		return ret;
	}
	
	protected static DefaultClientConfiguration getClientCfg() throws Exception
	{
		DefaultClientConfiguration sec = new DefaultClientConfiguration();
		sec.setCredential(new KeystoreCredential("src/test/resources/demouser.p12", 
				"the!user".toCharArray(), "the!user".toCharArray(), "demouser", "pkcs12"));
		sec.setValidator(new KeystoreCertChainValidator("src/test/resources/truststore.jks", 
				"unicore".toCharArray(), "jks", -1));
		sec.setSslAuthn(true);
		sec.setSslEnabled(true);
		sec.getETDSettings().setIssuerCertificateChain(sec.getCredential().getCertificateChain());
		sec.getHttpClientProperties().setProperty(HttpClientProperties.SO_TIMEOUT, "600000");
		sec.getHttpClientProperties().setProperty(HttpClientProperties.CONNECT_TIMEOUT, "5000");
		return sec;
	}
	
	/**
	 * Tests whether first accessible works in the chain:
	 * LDAP UVOS-Pull XUUDB File
	 * where 3 first have inaccessible server configured.
	 */
	public void testFirstAccessible()
	{
		System.out.println("Starting USE...");
		try
		{
			kernel= new Kernel("src/test/resources/use.properties", getProperties());
			kernel.startSynchronous();
			ForwardService.createInstance(kernel);
			Thread.sleep(3000);
			DefaultClientConfiguration sec = getClientCfg();
			
			String userDN = sec.getCredential().getCertificate().getSubjectX500Principal().getName();
			assertTrue(userDN.contains("Demo User"));
			
			String url=kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
			url += "/services/" + ForwardService.TEST_SERVICE + "?res=default";
			EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
			epr.addNewAddress().setStringValue(url);

			System.out.println("\nPERFORMING AN ACTUAL REQUEST ...\n");
			
			BaseWSRFClient client = new BaseWSRFClient(url, epr, sec);
			client.getResourcePropertyDocument();

			BaseWSRFClient client2 = new BaseWSRFClient(url, epr, sec);
			client2.getResourcePropertyDocument();
		
			kernel.shutdown();
		} catch(Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
}
