package eu.unicore.services.pdp.local;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.Logger;
import org.herasaf.xacml.core.SyntaxException;
import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.policy.PolicyMarshaller;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.herasaf.xacml.core.utils.JAXBMarshallerConfiguration;
import org.xml.sax.SAXException;

import com.google.common.primitives.Longs;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.FilePropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * Initially loads policies, also able to reload them.<br>
 * Policies are read from a configurable directory. 
 * Directory contents is set of files, which should be either XACML 2.0 policies
 * or policy sets. Those policies are combined with a configurable combining algorithm.
 * @author golbi
 */
public class LocalPolicyStore
{
	public static final Logger log = Log.getLogger(Log.SECURITY, LocalPolicyStore.class);
	public static final String POLICY_ALG_DENY_OVERRIDES = "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides";
	public static final String SPOLICY_ALG_DENY_OVERRIDES = "deny-overrides";
	public static final String POLICY_ALG_PERMIT_OVERRIDES = "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:permit-overrides";
	public static final String SPOLICY_ALG_PERMIT_OVERRIDES = "permit-overrides";
	public static final String POLICY_ALG_FIRST_APPLICABLE = "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:first-applicable";
	public static final String SPOLICY_ALG_FIRST_APPLICABLE = "first-applicable";
	public static final String POLICY_ALG_ONLY_ONE = "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:only-one-applicable";
	public static final String SPOLICY_ALG_ONLY_ONE = "only-one-applicable";
	public static final String POLICY_ALG_ORDERED_DENY_OVERRIDES = "urn:oasis:names:tc:xacml:1.1:policy-combining-algorithm:ordered-deny-overrides";
	public static final String SPOLICY_ALG_ORDERED_DENY_OVERRIDES = "ordered-deny-overrides";
	public static final String POLICY_ALG_ORDERED_PERMIT_OVERRIDES = "urn:oasis:names:tc:xacml:1.1:policy-combining-algorithm:ordered-permit-overrides";
	public static final String SPOLICY_ALG_ORDERED_PERMIT_OVERRIDES = "ordered-permit-overrides";

	public static final String PREFIX = "localpdp.";
	public static final String DIR_KEY = "directory";
	public static final String COMBINING_ALG_KEY = "combiningAlg";
	public static final String WILDCARD_KEY = "filesWildcard";
	private Map<String, String> algKeys2FullNames;
	
	public static Map<String, PropertyMD> META = new HashMap<>();
	static 
	{
		META.put(DIR_KEY, new PropertyMD("conf/policies").setPath());
		META.put(COMBINING_ALG_KEY, new PropertyMD(POLICY_ALG_FIRST_APPLICABLE));
		META.put(WILDCARD_KEY, new PropertyMD("*.xml"));
	}
	
	protected FilePropertiesHelper cfg;
	private PolicyListener pdp;
	private String configFile;

	public LocalPolicyStore(PolicyListener pdp, String configurationFile) 
		throws IOException, SyntaxException, JAXBException, SAXException
	{
		//uhhh herasf developers are OO gurus...
		SimplePDPFactory.getSimplePDP();
		this.pdp = pdp;
		JAXBContext policyContext = JAXBContext.newInstance("org.herasaf.xacml.core.policy.impl");
		PolicyMarshaller.setJAXBContext(policyContext);
		JAXBMarshallerConfiguration jmc = new JAXBMarshallerConfiguration();
		jmc.setValidateParsing(true);
		//dirty trick to overcome stupid .getClass.getClassloader() in herasf code
		URL url = LocalPolicyStore.class.getResource("/local/xacmlpolicy-2.0.xsd");
		jmc.setSchemaByPath("url:" + url.toExternalForm());
		PolicyMarshaller.setJAXBMarshallerConfiguration(jmc);
		initPolicyNameMap();
		reload(configurationFile);
	}
	
	public void reload(String configurationFile) throws ConfigurationException, IOException, SyntaxException
	{
		
		if(this.configFile==null || !configurationFile.equals(this.configFile)) {
			this.cfg = new FilePropertiesHelper(PREFIX, configurationFile, META, log);
			this.configFile = configurationFile;
		}
		else{
			cfg.reload();
		}
		File dir = cfg.getFileValue(DIR_KEY, true);
		String talg = cfg.getValue(COMBINING_ALG_KEY);
		String alg = algKeys2FullNames.get(talg);
		if (alg == null)
			throw new ConfigurationException("Configured XACML policy combining algorithm " +
					talg + " is unknown.");
		String wildcard = cfg.getValue(WILDCARD_KEY);
		
		FilenameFilter filter = new WildcardFileFilter(wildcard);
		String []files = dir.list(filter);
		if (files.length == 0) {
			throw new IOException("Configured XACML policies repository " + 
					dir + " is empty");
		}
		
		byte[]hash = getDirectoryHash(dir, files);
		if(Arrays.equals(hash, directoryHash)){
			log.info("Policy files unchanged, skipping reload.");
			return;
		}
		directoryHash = hash;

		if (log.isDebugEnabled())
		{
			log.debug("Using policy directory: " + dir + 
				" with files matching " + wildcard +
				" (found " + files.length + " policies)");
			log.debug("Using policy combining algorithm: " + alg);
		}

		Arrays.sort(files);
		List<Evaluatable> policies = new ArrayList<>();
		for (String policyF: files)
		{
			File p = new File(dir.getAbsolutePath() + File.separator + policyF);
			try
			{
				Evaluatable policy = PolicyMarshaller.unmarshal(p);
				policies.add(policy);
			} catch(SyntaxException e)
			{
				throw new SyntaxException("Syntax error in file " 
						+ policyF, e);
			}
		}
		
		pdp.updateConfiguration(policies, alg);
	}
	
	private void initPolicyNameMap()
	{
		algKeys2FullNames = new HashMap<>();
		algKeys2FullNames.put(POLICY_ALG_DENY_OVERRIDES, POLICY_ALG_DENY_OVERRIDES);
		algKeys2FullNames.put(POLICY_ALG_FIRST_APPLICABLE, POLICY_ALG_FIRST_APPLICABLE);
		algKeys2FullNames.put(POLICY_ALG_ONLY_ONE, POLICY_ALG_ONLY_ONE);
		algKeys2FullNames.put(POLICY_ALG_ORDERED_DENY_OVERRIDES, POLICY_ALG_ORDERED_DENY_OVERRIDES);
		algKeys2FullNames.put(POLICY_ALG_ORDERED_PERMIT_OVERRIDES, POLICY_ALG_ORDERED_PERMIT_OVERRIDES);
		algKeys2FullNames.put(POLICY_ALG_PERMIT_OVERRIDES, POLICY_ALG_PERMIT_OVERRIDES);
		algKeys2FullNames.put(SPOLICY_ALG_DENY_OVERRIDES, POLICY_ALG_DENY_OVERRIDES);
		algKeys2FullNames.put(SPOLICY_ALG_FIRST_APPLICABLE, POLICY_ALG_FIRST_APPLICABLE);
		algKeys2FullNames.put(SPOLICY_ALG_ONLY_ONE, POLICY_ALG_ONLY_ONE);
		algKeys2FullNames.put(SPOLICY_ALG_ORDERED_DENY_OVERRIDES, POLICY_ALG_ORDERED_DENY_OVERRIDES);
		algKeys2FullNames.put(SPOLICY_ALG_ORDERED_PERMIT_OVERRIDES, POLICY_ALG_ORDERED_PERMIT_OVERRIDES);
		algKeys2FullNames.put(SPOLICY_ALG_PERMIT_OVERRIDES, POLICY_ALG_PERMIT_OVERRIDES);
	}
	
	byte[] directoryHash = new byte[0];

	private byte[] getDirectoryHash(File directory, String[] files){
		try{
			MessageDigest md=MessageDigest.getInstance("MD5");
			for(String f: files){
				computeDirHash(md, new File(directory, f));
			}
			return md.digest();
		}catch(Exception ex){
			log.warn("Error checking for XACML file(s) modification",ex);
		}
		return new byte[0];
	}

	private void computeDirHash(MessageDigest md, File file){
		if(file.isDirectory()){
			for(File f: file.listFiles()){
				computeDirHash(md, f);
			}
		}
		else{
			md.update(Longs.toByteArray(file.lastModified()));
		}
	}

}
