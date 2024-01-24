package eu.unicore.services.aip.xuudb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fzJuelich.unicore.xuudb.AddCertificateDocument;
import de.fzJuelich.unicore.xuudb.LoginDataType;
import de.fzj.unicore.xuudb.server.HttpsServer;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

public class TestXUUDBServer {

	XUUDBJSONAttributeSource xuudb;
	static HttpsServer xuudbServer = null;

	@BeforeClass
	public static void startXUUDBServer() throws Exception {
		File dir = new File("target/data");
		FileUtils.deleteDirectory(dir);
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/xuudb/xuudb_server.conf"));
		xuudbServer = new HttpsServer(p);
		xuudbServer.start();
		// add entry
		AddCertificateDocument e1 = AddCertificateDocument.Factory.newInstance();
		LoginDataType t1 = e1.addNewAddCertificate();
		t1.setGcID("TEST");
		t1.setRole("user");
		t1.setXlogin("demouser");
		t1.setProjects("hpc");
		t1.setToken("UID=demouser");
		xuudbServer.getAdminImpl().addCertificate(e1);
	}

	@Before
	public void setupEndpoint() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		xuudb = new XUUDBJSONAttributeSource();
		xuudb.setXuudbCache(false);
		xuudb.setXuudbGCID("TEST");
		xuudb.setXuudbHost("http://localhost");
		xuudb.setXuudbPort(14463);
		xuudb.configure("test", k);
		xuudb.updateXUUDBConnectionStatus();
		System.out.println("Status: " + xuudb.getConnectionStatusMessage());
	}

	@Test
	public void test1() throws IOException {
		System.out.println("test1");
		SubjectAttributesHolder ah = new SubjectAttributesHolder();
		SecurityTokens t = new SecurityTokens();
		t.setUserName("UID=demouser");
		t.setConsignorTrusted(true);
		ah = xuudb.getAttributes(t, ah);
		System.out.println(ah);
	}
}
