package eu.unicore.uas.pdp.argus;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;

import xmlbeans.oasis.xacml.x2.x0.policy.PolicySetDocument;
import xmlbeans.oasis.xacml.x2.x0.policy.PolicySetType;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyQueryDocument;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyStatementType;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.StatementAbstractType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseType;
import de.fzj.unicore.wsrflite.ThreadingServices;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.FilePropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.httpclient.IClientConfiguration;

public class ArgusPAPChecker {
	private static final Logger log = Log.getLogger(Log.SECURITY, ArgusPAPChecker.class);

	public static final String PREFIX = "argus.pap.";
	
	public static final String ADDR_KEY = "serverAddress";
	public static final String TIMEOUT_KEY = "queryTimeout";
	public static final String INTERVAL_KEY = "queryInterval";
	public static final String DENY_TIMEOUT_KEY = "denyTimeout";
	public static final String POLICY_FILENAME_KEY = "policysetFilename";

	private URL argusAddress;
	private int queryTimeout;
	private int queryInterval;
	private int denyTimeout;
	private ArgusPAPClient client;
	private XACMLPolicyQueryDocument req;
	private Object notification;
	private String argusFile;
	private long lastUpdate;
	private boolean denyAll;
	private ThreadingServices threadingSrv;

	public static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static 
	{
		META.put(ADDR_KEY, new PropertyMD("https://localhost:8150/pap/services/ProvisioningService"));
		META.put(TIMEOUT_KEY, new PropertyMD("5000"));
		META.put(INTERVAL_KEY, new PropertyMD("300000"));
		META.put(DENY_TIMEOUT_KEY, new PropertyMD("-1"));
		META.put(POLICY_FILENAME_KEY, new PropertyMD("argus_pap.xml").setPath());
	}

	
	
	public ArgusPAPChecker(String configurationFile, Object notificationObject,
			String baseUrl, IClientConfiguration sec, ThreadingServices threadingSrv)
			throws IOException {
		this.threadingSrv = threadingSrv;
		loadConfiguration(configurationFile);
		client = new ArgusPAPClient(argusAddress, queryTimeout, sec);
		req = PolicyRequestCreator.createSAMLPolicyQuery(baseUrl);
		// SimplePDPFactory.getSimplePDP();
		notification = notificationObject;
	}

	private void loadConfiguration(String configurationFile) throws ConfigurationException, IOException {
		FilePropertiesHelper cfg = new FilePropertiesHelper(PREFIX, configurationFile, META, log);
		String addr = cfg.getValue(ADDR_KEY);
		try {
			argusAddress = new URL(addr);
		} catch (MalformedURLException e) {
			throw new ConfigurationException("Argus PAP URL ('" + addr + "') is invalid: "
					+ e);
		}

		queryTimeout = cfg.getIntValue(TIMEOUT_KEY);
		queryInterval = cfg.getIntValue(INTERVAL_KEY);
		denyTimeout = cfg.getIntValue(DENY_TIMEOUT_KEY);
		
		String policyFilename = cfg.getValue(POLICY_FILENAME_KEY);

		String policiesDir = cfg.getRawProperty(ArgusHerasafPolicyStore.PREFIX+ArgusHerasafPolicyStore.DIR_KEY);
		if (policiesDir == null)
			policiesDir = ArgusHerasafPolicyStore.META.get(ArgusHerasafPolicyStore.DIR_KEY).getDefault();

		argusFile = policiesDir + File.separator + policyFilename;
		if (log.isDebugEnabled()) {
			log.debug("ArgusPAPChecker loaded configuration from "
					+ configurationFile);
			log.debug("ArgusPAPChecker query interval time is set to "
					+ queryInterval);
		}
	}

	private PolicySetDocument processResponse(ResponseDocument respDoc)
			throws Exception {
		PolicySetDocument doc = null;
		ResponseType resp = respDoc.getResponse();
		AssertionType[] respAssertions = resp.getAssertionArray();
		if (respAssertions == null || respAssertions.length == 0)
			throw new Exception(
					"Argus service error: got response (not a fault) without a SAML assertion");

		StatementAbstractType[] statements = respAssertions[0]
				.getStatementArray();
		if (statements == null || statements.length == 0)
			throw new Exception(
					"Argus PAP service error: response's SAML assertion has no XACML statement inside.");

		for (StatementAbstractType statement : statements) {
			if (!(statement instanceof XACMLPolicyStatementType)) {
				throw new Exception(
						"Argus PAP service error: response's SAML assertion has statement should"
								+ " be XACMLPolicyStatmentType, while is of "
								+ statements[0].schemaType().getName());
			} else {
				XACMLPolicyStatementType xp = (XACMLPolicyStatementType) statement;
				PolicySetType[] array = xp.getPolicySetArray();
				if (array.length > 1)
					log
							.warn("Argus PAP send more than 1 policySet, check configuration");
				doc = PolicySetDocument.Factory.newInstance();
				doc.setPolicySet(array[0]);

			}

		}
		return doc;
	}

	private void argusPAPQuery()
	{
		try {
			if (log.isDebugEnabled())
				log.debug("XACML request for Argus PAP:\n"
						+ req.xmlText(new XmlOptions().setSavePrettyPrint()));

			ResponseDocument respDoc = client.sendRequest(req);

			if (log.isDebugEnabled())
				log.debug("XACML answer from Argus PAP:\n"
						+ respDoc.xmlText(new XmlOptions().setSavePrettyPrint()));

			PolicySetDocument doc = processResponse(respDoc);

			if (!comparePolicySet(doc)) {
				if (log.isDebugEnabled())
					log.debug("Save new policySet in argus file: " + argusFile);
				doc.save(new File(argusFile), new XmlOptions().setSavePrettyPrint());
				synchronized (notification) {
					notification.notifyAll();
				}
			}

			if (denyTimeout > 0) {
				synchronized (this) {
					if (denyAll) {
						log.info("DENY ALL mode OFF");
						denyAll = false;
					}
				}
			}

			lastUpdate = System.currentTimeMillis();

		} catch (Exception e) {
			log.error("Argus PAP callout error, check connection or start argus pap server:", e);

			if (denyTimeout > 0) {
				synchronized (this) {
					if (!denyAll)
						if (System.currentTimeMillis() - lastUpdate > denyTimeout) {
							log.info("DENY ALL mode ON. Argus PAP did not respond for more than "
											+ denyTimeout + " ms");
							denyAll = true;
						}
				}
			}
		}		
	}
	
	public void start() {
		lastUpdate = System.currentTimeMillis();
		Runnable runner = new Runnable() {
			public void run() {
				argusPAPQuery();
			}
		};

		log.info("ArgusPAPChecker started");
		threadingSrv.getScheduledExecutorService().scheduleWithFixedDelay(
				runner, queryInterval, queryInterval, TimeUnit.MILLISECONDS);

	}

	public boolean isDenyAllMode() {
		synchronized (this) {
			return denyAll;
		}

	}

	private boolean comparePolicySet(PolicySetDocument doc) throws Exception {
		boolean ans = false;

		File f = new File(argusFile);

		if (log.isDebugEnabled())
			log.debug("Comparing local policy from file: "+argusFile+ " and Argus PAP policy");

		if (!f.exists()) {
			ans = false;

		} else {

			PolicySetDocument document;
			try {
				document = PolicySetDocument.Factory.parse(f);
				ans = doc.toString().equals(document.toString());
			} catch (Exception e) {
				throw new Exception("Cannot parse policy xml file " + argusFile);
			}

		}
		if (log.isDebugEnabled())
			if (ans)
				log.debug("Policy in " + argusFile
						+ " is the same as Argus PAP server policy");
			else
				log.debug("Policy in " + argusFile
						+ " is not the same as Argus PAP server policy");

		return ans;
	}
}
