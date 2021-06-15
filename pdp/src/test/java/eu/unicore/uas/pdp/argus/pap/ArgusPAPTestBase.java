package eu.unicore.uas.pdp.argus.pap;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.services.ContainerProperties;
import eu.unicore.uas.pdp.argus.ArgusPAP;
import eu.unicore.util.httpclient.DefaultClientConfiguration;


/*
 * Before Argus test:
 *1. Configure Argus PAP server, use sample conf file from:
 *	a)src/test/resources/argus/papserver/conf
 *	   --edit pap_configuration.ini,set proper certs path 
 *  b)src/test/resources/argus/papserver/certs
 *
 *2. Before argus unit test you must add policy from appropriate spl file.
 *   The path of spl file is written in each comment of argus unit tests. 
 */
public class ArgusPAPTestBase {

	private static String tPolicy = "target/policies";
	private static String sPolicy = "src/test/resources/argus/papserver/policy_xml";
	protected static ArgusPAP pap;
	

	public static final String KS = "src/test/resources/argus/client/localhost.jks";
	public static final String KS_PASSWD = "localhost";
	public static final String KS_ALIAS = "localhost";

	@Before
	public void setup() throws InterruptedException {


		clearPolicyDir();
		addDefaultUnicorePolicy();

		DefaultClientConfiguration clientConfiguration = new DefaultClientConfiguration();

		clientConfiguration.setSslEnabled(true);
		clientConfiguration.setSslAuthn(true);

		try {
			clientConfiguration.setCredential(new KeystoreCredential(KS,
					KS_PASSWD.toCharArray(), KS_PASSWD.toCharArray(), KS_ALIAS,
					"JKS"));

			clientConfiguration.setValidator(new KeystoreCertChainValidator(KS,
					KS_PASSWD.toCharArray(), "JKS", -1));

		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {

			pap = new ArgusPAP();

			ContainerProperties cp = new ContainerProperties(new Properties(),
					false);

			pap.initialize("src/test/resources/argus/argus.pap.conf", cp, null,
					clientConfiguration);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void addDefaultUnicorePolicy() {

		File fdir = new File(tPolicy);
		if (!fdir.exists()) {
			fdir.mkdir();
		}

		File f1 = new File(sPolicy);
		File f2 = new File(tPolicy);
		System.out.println("Add default policies from " + sPolicy);
		try {
			FileUtils.copyDirectory(f1, f2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clearPolicyDir() {

		System.out.println("Clear policy dir " + tPolicy);

		File f = new File(tPolicy);
		if (f.exists()) {

			try {
				FileUtils.cleanDirectory(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
