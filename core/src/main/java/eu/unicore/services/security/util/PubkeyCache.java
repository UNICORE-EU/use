package eu.unicore.services.security.util;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.services.Kernel;
import eu.unicore.util.Log;

/**
 * stores public keys by subject, used for validating JWT assertions
 */
public class PubkeyCache {

	private static final Logger logger = Log.getLogger(Log.SECURITY,PubkeyCache.class);

	private final Map<String, Collection<PublicKey>>map = new ConcurrentHashMap<>();

	public synchronized Collection<PublicKey> getPublicKeys(String subject){
		subject = X500NameUtils.getComparableForm(subject);
		Collection<PublicKey> keys = map.get(subject);
		if(keys==null) {
			keys = new HashSet<PublicKey>();
			map.put(subject, keys);
		}
		return keys;
	}

	private void add(String subject, PublicKey pubkey){
		getPublicKeys(subject).add(pubkey);
	}

	// 10 days of grace period added to certificate's NotAfter date
	private static long gracePeriod = 10*24*3600;

	/**
	 * stores the public key, if the certificate is not expired
	 * @param certificate
	 * @return true if certificate was added, false if not (it is expired)
	 */
	public boolean update(X509Certificate certificate){
		long notAfter = certificate.getNotAfter().getTime();
		String subject = certificate.getSubjectX500Principal().getName();
		if(System.currentTimeMillis()<notAfter+gracePeriod) {
			add(subject, certificate.getPublicKey());
			logger.debug("Added trusted certificate for <{}> for validating authentication assertions.",
					subject);
			return true;
		}
		logger.warn("Expired certificate for <{}>, no longer trusted for validating authentication assertions.",
				subject);
		return false;
	}

	public static synchronized PubkeyCache get(Kernel kernel){
		PubkeyCache pc = kernel.getAttribute(PubkeyCache.class);
		if(pc==null){
			pc = new PubkeyCache();
			kernel.setAttribute(PubkeyCache.class, pc);
		}
		return pc;
	}
}