package eu.unicore.services.aip.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

/**
 * @author golbi
 */
public class TestFileAttributeSource
{
	public static final String NAME = "TST1";

	private FileAttributeSource init(String file, String matching, String format) throws Exception
	{
		FileAttributeSource src = new FileAttributeSource();
		src.setFile(file);
		src.setMatching(matching);
		src.setFormat(format);
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		src.configure(NAME, k);
		assertTrue(NAME.equals(src.getName()));
		return src;
	}

	@Test
	public void testStrict() throws Exception {
		FileAttributeSource src = init("src/test/resources/file/testUudb-strict.xml", "strict", null);

		SecurityTokens tokens = new SecurityTokens();
		tokens.setUserName("CN=Stanisław Lem, C=PL");
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

		tokens.setUserName("CN=Dead Man, C=US");
		holder = src.getAttributes(tokens, null);
		valid = holder.getValidIncarnationAttributes();
		def = holder.getDefaultIncarnationAttributes();
		assertTrue(valid.size() == 1 && def.size() == 1);
		assertTrue(valid.get("role") != null && valid.get("role").length == 1
				&& valid.get("role")[0].equals("user1"));
		assertTrue(def.get("role") != null && def.get("role").length == 1
				&& def.get("role")[0].equals("user1"));

		tokens.setUserName("C=US, CN=Dead Man");
		holder = src.getAttributes(tokens, null);
		valid = holder.getValidIncarnationAttributes();
		def = holder.getDefaultIncarnationAttributes();
		assertTrue(valid.size() == 0);
		assertTrue(def.size() == 0);
	}

	@Test
	public void testRegExp() throws Exception {
		FileAttributeSource src = init("src/test/resources/file/testUudb-regexp.xml", "regexp", null);

		SecurityTokens tokens = new SecurityTokens();
		tokens.setConsignorTrusted(true);
		SubjectAttributesHolder map;

		tokens.setUserName("CN=Stanisław Lem, O=ICM, C=PL");
		map = src.getAttributes(tokens, null);
		assertTrue(map.getValidIncarnationAttributes().size() == 2);

		tokens.setUserName("CN=Stanisław Lem, O=ACK, C=PL");
		map = src.getAttributes(tokens, null);
		assertTrue(map.getValidIncarnationAttributes().size() == 1);

		tokens.setUserName("CN=Stanisław Lem, O=I, C=PL");
		map = src.getAttributes(tokens, null);
		assertTrue(map.getValidIncarnationAttributes().size() == 2);

		tokens.setUserName("CN=Dead Man, C=US");
		map = src.getAttributes(tokens, null);
		assertTrue(map.getValidIncarnationAttributes().size() == 1);
	}


	@Test
	public void testRefresh() throws Exception {
		String srcF = "src/test/resources/file/testUudb-strict.xml";
		String dst = "target/test-classes/testUudb-copy.xml";
		File f = new File(dst);
		f.delete();
		FileUtils.copyFile(new File(srcF), new File(dst), false);
		
		FileAttributeSource src = init(dst, "regexp", null);

		SecurityTokens tokens = new SecurityTokens();
		tokens.setConsignorTrusted(true);
		SubjectAttributesHolder holder;
		Map<String, String[]> def;

		tokens.setUserName("CN=Dead Man, C=US");
		holder = src.getAttributes(tokens, null);
		def = holder.getDefaultIncarnationAttributes();

		assertTrue(def.size() == 1);
		assertTrue(def.get("role") != null && def.get("role").length == 1
				&& def.get("role")[0].equals("user1"));
		Thread.sleep(1000);
		FileUtils.copyFile(new File("src/test/resources/file/testUudb-regexp.xml"), 
				new File(dst), false);

		holder = src.getAttributes(tokens, null);
		def = holder.getDefaultIncarnationAttributes();
		assertTrue(def.size() == 1);
		assertTrue(def.get("role") != null && def.get("role").length == 1
				&& def.get("role")[0].equals("banned"));

	}

	@Test
	public void testDetectFormat() throws Exception {
		FileAttributeSource src = new FileAttributeSource();
		String f = "src/test/resources/file/testUudb-strict.xml";
		assertEquals("XML", src.detectFormat(new File(f)));
		f = "src/test/resources/file/testUudb.json";
		assertEquals("JSON", src.detectFormat(new File(f)));
		
	}

}
