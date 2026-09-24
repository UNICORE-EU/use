package eu.unicore.services.aip.xuudb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.xuudb.interfaces.IPublic;
import io.imunity.tanl.x509.impl.CertificateUtils;
import io.imunity.tanl.x509.impl.CertificateUtils.Encoding;

public class TestXUUDBAuthoriser {

	XUUDBAttributeSource xuudb;
	MockXUUDB mock;
	
	@BeforeEach
	public void setUp()throws Exception{
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

	@Test
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
