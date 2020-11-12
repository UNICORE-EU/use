package eu.unicore.uas.pdp.argus;

import java.io.IOException;

import javax.naming.ConfigurationException;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.Logger;
import org.herasaf.xacml.core.SyntaxException;
import org.xml.sax.SAXException;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.uas.pdp.local.LocalHerasafPDP;
import eu.unicore.uas.pdp.request.creator.HerasafXacml2RequestCreator;
import eu.unicore.uas.pdp.request.profile.EMI1Profile;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

public class ArgusPAP extends LocalHerasafPDP {
	private static final Logger log = Log.getLogger(Log.SECURITY,
			ArgusPAP.class);
	private ArgusPAPChecker checker;

	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws IOException,
			SyntaxException, JAXBException, SAXException, ConfigurationException {
		if (configuration == null)
			throw new ConfigurationException("For " + ArgusPAP.class.getName() + 
					" PDP a configuration file must be defined.");
		String baseUrl = baseSettings.getValue(ContainerProperties.WSRF_BASEURL);
		super.initialize(new HerasafXacml2RequestCreator(new EMI1Profile(baseUrl)));
		Object notificationObject = new Object();
		new ArgusHerasafPolicyStore(this, configuration, 
				notificationObject, baseSettings.getThreadingServices());
		checker = new ArgusPAPChecker(configuration, notificationObject, 
			baseUrl, clientConfiguration, baseSettings.getThreadingServices());
		checker.start();

	}

	public PDPResult checkAuthorisation(Client c, ActionDescriptor action,
			ResourceDescriptor d) throws Exception {
		if (!checker.isDenyAllMode()) {
			return super.checkAuthorisation(c, action, d);
		} else {
			if (log.isDebugEnabled()) {

				log.debug("User: " + c.getDistinguishedName()
						+ " are banned, (DENY ALL MODE ON)");
			}
			return new PDPResult(PDPResult.Decision.DENY,
					"PDP DENY ALL MODE ON");
		}

	}
}
