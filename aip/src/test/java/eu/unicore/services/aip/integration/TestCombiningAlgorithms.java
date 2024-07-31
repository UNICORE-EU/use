package eu.unicore.services.aip.integration;

import static eu.unicore.services.security.ContainerSecurityProperties.PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_COMBINING_POLICY;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_ORDER;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.aip.file.FileAttributeSource;
import eu.unicore.services.aip.ldap.LDAPAttributeSource;
import eu.unicore.services.aip.saml.SAMLAttributeSource;
import eu.unicore.services.aip.xuudb.XUUDBAttributeSource;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;

public class TestCombiningAlgorithms {
	protected Kernel kernel;

	protected Properties getProperties()
	{
		Properties ret = new Properties();
		ret.setProperty(PREFIX+ContainerSecurityProperties.PROP_CHECKACCESS_PDP, "eu.unicore.services.aip.integration.MockPDP");

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

		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".VO-PULL.class", SAMLAttributeSource.class.getName());
		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".VO-PULL.configurationFile", "src/test/resources/vo-pull.config");

		ret.setProperty(PREFIX+PROP_AIP_PREFIX+".XUUDB.class", XUUDBAttributeSource.class.getName());
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
		sec.getHttpClientProperties().setProperty(HttpClientProperties.SO_TIMEOUT, "600000");
		sec.getHttpClientProperties().setProperty(HttpClientProperties.CONNECT_TIMEOUT, "5000");
		return sec;
	}

	/**
	 * Tests whether first accessible works in the chain:
	 * LDAP UVOS-Pull XUUDB File
	 * where 3 first have inaccessible server configured.
	 */
	@Test
	public void testFirstAccessible() throws Exception {
		kernel= new Kernel("src/test/resources/use.properties", getProperties());
		kernel.startSynchronous();
		DefaultClientConfiguration sec = getClientCfg();

		String userDN = sec.getCredential().getCertificate().getSubjectX500Principal().getName();
		assertTrue(userDN.contains("Demo User"));

		IAttributeSource aip = kernel.getSecurityManager().getAip();
		SecurityTokens st = new SecurityTokens();
		st.setUserName(userDN);
		st.setConsignorTrusted(true);
		SubjectAttributesHolder sah = aip.getAttributes(st, new SubjectAttributesHolder());

		String [] rl = sah.getIncarnationAttributes().get("role");
		assertTrue(rl!=null);
		assertEquals("user", rl[0]);

		kernel.shutdown();
	}
}
