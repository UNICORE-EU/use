package eu.unicore.services.aip.xuudb;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;

import de.fzj.unicore.xuudb.interfaces.IPublic;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.emi.security.authn.x509.proxy.ProxyCertificate;
import eu.emi.security.authn.x509.proxy.ProxyCertificateOptions;
import eu.emi.security.authn.x509.proxy.ProxyGenerator;
import eu.emi.security.authn.x509.proxy.ProxyUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.TestConfigUtil;
import junit.framework.TestCase;

public class TestXUUDBAuthoriser extends TestCase{

	XUUDBAttributeSource xuudb;
	MockXUUDB mock;
	
	@Override
	protected void setUp()throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		mock=new MockXUUDB();
		xuudb=new XUUDBAttributeSource() {
			@Override
			protected IPublic createEndpoint() {
				return mock;
			}
		};
		xuudb.setXuudbCache(false);
		xuudb.configure("test",k);
	}

	public void testCheckProxyDN()throws Exception{
		X509Credential cred = new KeystoreCredential("src/test/resources/xuudb/user-keystore.jks",
				"the!user".toCharArray(), "the!user".toCharArray(), "demo user", "jks");
		ProxyCertificateOptions opts = new ProxyCertificateOptions(cred.getCertificateChain());
		ProxyCertificate proxy=ProxyGenerator.generate(opts, cred.getKey());
		
		SecurityTokens tokens=new SecurityTokens();
		
		tokens.setUser(new X509Certificate[] {ProxyUtils.getEndUserCertificate(proxy.getCertificateChain())});
		tokens.setConsignor(proxy.getCertificateChain());
		tokens.setConsignorTrusted(true);
		
		String userName = cred.getCertificate().getSubjectX500Principal().getName();
		mock.expectedDN = userName;
		SubjectAttributesHolder attr=xuudb.getAttributes(tokens, null);
		assertNotNull(attr);
		assertTrue(mock.callCount>0);
		assertEquals(userName,mock.lastDN);
	}
	
	public void testCheckDNResult()throws Exception{
		SecurityTokens tokens=new SecurityTokens();
		X509Certificate[] cert=CertificateUtils.loadCertificateChain(
				new FileInputStream("src/test/resources/xuudb/user-cert.pem"), Encoding.PEM);
		tokens.setUser(cert);
		tokens.setConsignorTrusted(true);
		String userName=cert[0].getSubjectX500Principal().getName();
		mock.expectedDN=userName;
		mock.xlogin="test1::test2";
		mock.role="user:admin";
		mock.projects="p1:p2:p3";
		
		SubjectAttributesHolder attr=xuudb.getAttributes(tokens, null);
		assertNotNull(attr);
		assertTrue(mock.callCount>0);
		assertEquals(userName,mock.lastDN);
		
		assertEquals(2,attr.getValidIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_XLOGIN).length);
		assertEquals(3,attr.getValidIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_GROUP).length);
		assertEquals(2,attr.getValidIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_ROLE).length);
		assertEquals(1,attr.getDefaultIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_XLOGIN).length);
		assertEquals(1,attr.getDefaultIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_GROUP).length);
		assertEquals(1,attr.getDefaultIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_ROLE).length);
		
		assertEquals("user",attr.getDefaultIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_ROLE)[0]);
			
	}
	
}
