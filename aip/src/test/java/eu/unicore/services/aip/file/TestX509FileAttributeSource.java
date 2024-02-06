package eu.unicore.services.aip.file;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.junit.Test;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

/**
 * @author golbi
 */
public class TestX509FileAttributeSource {
	public static final String NAME = "TST1";


	private X509FileAttributeSource init(String file)
	{
		X509FileAttributeSource src = new X509FileAttributeSource();
		src.setFile(file);
		try
		{
			Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
			src.configure(NAME, k);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Can't init AttributeSource: " + e);
		}

		assertTrue(NAME.equals(src.getName()));
		return src;
	}

	@Test
	public void test1()throws Exception
	{
		X509FileAttributeSource src = init("src/test/resources/file/testUudb-full-x509.xml");

		SecurityTokens tokens = new SecurityTokens();
		X509Certificate[] c = CertificateUtils.loadCertificateChain(
				new FileInputStream("src/test/resources/file/demoadmin.pem"), Encoding.PEM);
		tokens.setUser(c);
		tokens.setConsignor(c);
		tokens.setConsignorTrusted(true);
		SubjectAttributesHolder holder = src.getAttributes(tokens, null);
		Map<String, String[]> valid = holder.getValidIncarnationAttributes();
		Map<String, String[]> def = holder.getDefaultIncarnationAttributes();
		assertTrue(valid.size() == 2);
		assertTrue(valid.get("role") != null && valid.get("role").length == 1
				&& valid.get("role")[0].equals("user"));
		assertTrue(valid.get("xlogin") != null && valid.get("xlogin").length == 2
				&& valid.get("xlogin")[0].equals("somebody")
				&& valid.get("xlogin")[1].equals("nobody"));
		assertTrue(valid.get("empty") == null);

		assertTrue(holder.getXacmlAttributes().size() == 2);

		assertTrue(def.size() == 2);
		assertTrue(def.get("role") != null && def.get("role").length == 1
				&& def.get("role")[0].equals("user"));
		assertTrue(def.get("xlogin") != null && def.get("xlogin").length == 1
				&& def.get("xlogin")[0].equals("somebody"));
		assertTrue(def.get("empty") == null);
	}
}
