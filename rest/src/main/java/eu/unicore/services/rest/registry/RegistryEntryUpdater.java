package eu.unicore.services.rest.registry;

import java.util.Calendar;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.impl.InstanceChecker;
import eu.unicore.services.registry.RegistryEntryImpl;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.util.Log;

/**
 * Checks and refreshes a registry entry in the local registry.
 * 
 * @author demuth
 * @author schuller
 */
public class RegistryEntryUpdater implements InstanceChecker {

	private static final Logger logger = Log.getLogger(Log.SERVICES + ".registry", RegistryEntryUpdater.class);

	private final Kernel kernel;
	
	public RegistryEntryUpdater(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public boolean check(Home home, String id) throws Exception {
		Calendar c = home.getTerminationTime(id);
		logger.debug("Checking <{} {}> TT = {}", home.getServiceName(), id, (c != null ? c.getTime() : "none"));
		// if for some reason the TT is null, force a refresh (in contrast to the usual
		// expiry check)
		return c == null ? true : (c.compareTo(Calendar.getInstance()) <= 0);
	}

	/**
	 * For the current registry entry, it is checked whether the corresponding
	 * member service (i.e. the service registered in the local registry) is still
	 * alive.
	 * 
	 * <ul>
	 * <li>If the member service is no longer alive, its entry is refreshed, i.e.
	 * re-added to the local registry.
	 *
	 * <li>If the member service is gone, the registry entry is removed.
	 * </ul>
	 *
	 * @return <code>true</code> if the registry entry is still valid,
	 *         <code>false</code> if the service is gone and the registry entry was
	 *         removed
	 */
	@Override
	public boolean process(Home home, String id) {
		if (home.isShuttingDown())
			return true;
		String serviceName = home.getServiceName();
		try (RegistryEntryImpl entry = (RegistryEntryImpl) home.getForUpdate(id)) {
			String memberAddress = entry.getModel().getEndpoint();
			// check that URL is still basically correct (e.g. hostname, port)
			if (!checkBasicCorrectness(memberAddress, kernel)) {
				logger.info("Member address <{}> is no longer valid, destroying registry entry.", memberAddress);
				entry.destroy();
				// instance is invalid and should be removed from all checks
				return false;
			}
			Map<String, String> content = entry.getModel().getContent();
			// if server key is present in content, check that it is still up to date
			checkAndUpdateServerPublicKey(kernel, content);
			try {
				reAdd(kernel, memberAddress, content);
				logger.debug("Refreshed registry entry for: {}", memberAddress);
			} catch (Exception e) {
				Log.logException("Error refreshing service entry for: " + memberAddress, e, logger);
			}
		} catch (ResourceUnknownException rue) {
			// entry is gone
			return false;
		} catch (Exception e) {
			Log.logException("Could not update registry entry " + serviceName + "/" + id, e, logger);
		}
		// instance is still valid
		return true;
	}

	/**
	 * 
	 * @param url - the member service URL to check
	 * @return <code>true</code> if URL looks OK
	 */
	private boolean checkBasicCorrectness(String url, Kernel k) {
		String baseURL = k.getContainerProperties().getContainerURL();
		return url != null && url.startsWith(baseURL);
	}

	private void checkAndUpdateServerPublicKey(Kernel k, Map<String, String> content) {
		String dn = content.get(RegistryClient.SERVER_IDENTITY);
		String pem = content.get(RegistryClient.SERVER_PUBKEY);
		if (dn != null && pem != null) {
			String dn1 = k.getContainerSecurityConfiguration().getCredential().getSubjectName();
			String pem1 = k.getContainerSecurityConfiguration().getServerCertificateAsPEM();
			content.put(RegistryClient.SERVER_IDENTITY, dn1);
			content.put(RegistryClient.SERVER_PUBKEY, pem1);
		}
	}

	private void reAdd(Kernel kernel, String endpoint, Map<String, String> content) throws Exception {
		RegistryHandler handler = kernel.getAttribute(RegistryHandler.class);
		handler.getRegistryClient().refresh(endpoint, content);
	}

}
