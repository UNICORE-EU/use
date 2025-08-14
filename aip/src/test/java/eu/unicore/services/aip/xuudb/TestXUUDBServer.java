package eu.unicore.services.aip.xuudb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.ExternalSystemConnector.Status;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.xuudb.server.HttpsServer;
import eu.unicore.xuudb.xbeans.AddCertificateDocument;
import eu.unicore.xuudb.xbeans.LoginDataType;

public class TestXUUDBServer {
	
	XUUDBAttributeSource xuudb;
	XUUDBJSONAttributeSource jxuudb;
	static HttpsServer xuudbServer = null;

	@BeforeAll
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

	@BeforeEach
	public void setupEndpoint() throws Exception{
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		jxuudb = new XUUDBJSONAttributeSource();
		jxuudb.setXuudbCache(false);
		jxuudb.setXuudbGCID("TEST");
		jxuudb.setXuudbHost("http://localhost");
		jxuudb.setXuudbPort(14463);
		jxuudb.configure("XUUDB-JSON", k);
		jxuudb.updateXUUDBConnectionStatus();
		System.out.println(jxuudb+": " + jxuudb.getConnectionStatusMessage());
		assertEquals(Status.OK, jxuudb.getConnectionStatus());
		
		xuudb = new XUUDBAttributeSource();
		xuudb.setXuudbCache(false);
		xuudb.setXuudbGCID("TEST");
		xuudb.setXuudbHost("http://localhost");
		xuudb.setXuudbPort(14463);
		xuudb.configure("XUUDB", k);
		xuudb.updateXUUDBConnectionStatus();
		System.out.println(xuudb+": " + xuudb.getConnectionStatusMessage());
		assertEquals(Status.OK, xuudb.getConnectionStatus());
	}

	@Test
	public void testJSON() throws IOException {
		SubjectAttributesHolder ah = new SubjectAttributesHolder();
		SecurityTokens t = new SecurityTokens();
		t.setUserName("UID=demouser");
		t.setConsignorTrusted(true);
		ah = jxuudb.getAttributes(t, ah);
		System.out.println("Attributes from "+jxuudb.getExternalSystemName()+": "+ah);
	}
	
	@Test
	public void testXML() throws IOException {
		SubjectAttributesHolder ah = new SubjectAttributesHolder();
		SecurityTokens t = new SecurityTokens();
		t.setUserName("UID=demouser");
		t.setConsignorTrusted(true);
		ah = xuudb.getAttributes(t, ah);
		System.out.println("Attributes from "+xuudb.getExternalSystemName()+": "+ah);
	}
}
