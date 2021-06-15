package eu.unicore.uas.security.xuudb;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.TestConfigUtil;
import junit.framework.TestCase;

public class TestDAPAuthoriser extends TestCase {
	XUUDBDynamicAttributeSource xuudb;
	MockDAP mock;

	@Override
	protected void setUp() throws Exception {
		Kernel k = new Kernel(TestConfigUtil.getInsecureProperties());
		xuudb = new XUUDBDynamicAttributeSource();
		mock = new MockDAP();
		xuudb.setDAPEndpoint(mock);
		xuudb.configure("test");
		xuudb.start(k);
	}

	public void testGetAttr() throws Exception {
		SecurityTokens tokens = new SecurityTokens();
		X509Certificate[] cert = CertificateUtils.loadCertificateChain(
				new FileInputStream("src/test/resources/xuudb/user-cert.pem"),
				Encoding.PEM);
		tokens.setUser(cert);
		tokens.setConsignorTrusted(true);
		Client cl = new Client();
		cl.setAuthenticatedClient(tokens);
		xuudb.setXuudbCache(false);
		
		mock.xlogin = "xlogin1";
		mock.gid = "gid1";
		mock.supplementaryGids = new String[] { "sgid1", "sgid2" };

		SubjectAttributesHolder attr = xuudb.getAttributes(cl, null);
		assertNotNull(attr);
		assertTrue(mock.callCount > 0);

		assertEquals(1, attr.getValidIncarnationAttributes().get(
				IAttributeSource.ATTRIBUTE_GROUP).length);
		assertEquals(2, attr.getValidIncarnationAttributes().get(
				IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS).length);
		assertEquals(1, attr.getValidIncarnationAttributes().get(
				IAttributeSource.ATTRIBUTE_XLOGIN).length);

		assertEquals("gid1", attr.getDefaultIncarnationAttributes().get(
				IAttributeSource.ATTRIBUTE_GROUP)[0]);
		assertEquals("xlogin1", attr.getDefaultIncarnationAttributes().get(
				IAttributeSource.ATTRIBUTE_XLOGIN)[0]);
		assertEquals("sgid1", attr.getDefaultIncarnationAttributes().get(
				IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS)[0]);

	}

	public void testCache() throws Exception {
		SecurityTokens tokens = new SecurityTokens();
		X509Certificate[] cert = CertificateUtils.loadCertificateChain(
				new FileInputStream("src/test/resources/xuudb/user-cert.pem"),
				Encoding.PEM);
		tokens.setUser(cert);
		tokens.setConsignorTrusted(true);
		Client cl = new Client();
		cl.setAuthenticatedClient(tokens);

		Client cl2 = new Client();
		cl2.setAuthenticatedClient(tokens);
		xuudb.setXuudbCache(false);
		
		mock.xlogin = "";
		mock.gid = "";
		mock.supplementaryGids = null;

		xuudb.getAttributes(cl, null);

		assertEquals(0, xuudb.getCacheSize());
		
		xuudb.setXuudbCache(true);
		mock.xlogin = "xlogin1";
		mock.gid = "gid1";
		mock.supplementaryGids = new String[] { "sgid1", "sgid2" };

		xuudb.getAttributes(cl, null);
		assertEquals(1, xuudb.getCacheSize());

		xuudb.getAttributes(cl, null);
		assertEquals(1, xuudb.getCacheSize());

		xuudb.getAttributes(cl2, null);
		assertEquals(1, xuudb.getCacheSize());

		cl2.setVos(new String[] { "x", "x2" });
		cl2.setVo("x");

		xuudb.getAttributes(cl2, null);
		assertEquals(2, xuudb.getCacheSize());

		Client cl3 = new Client();
		cl3.setAuthenticatedClient(tokens);
		Xlogin xlogin = new Xlogin(new String[] { "user" });
		cl3.setXlogin(xlogin);

		xuudb.getAttributes(cl3, null);
		assertEquals(3, xuudb.getCacheSize());
		
		
		

	}

}
