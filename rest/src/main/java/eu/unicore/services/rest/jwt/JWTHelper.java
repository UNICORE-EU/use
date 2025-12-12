package eu.unicore.services.rest.jwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.Client;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.util.Log;

public class JWTHelper {

	private static final Logger logger = Log.getLogger(Log.SECURITY, JWTHelper.class);

	private final JWTServerProperties preferences;

	private final IContainerSecurityConfiguration securityProperties;

	private final String serverDN;

	private final PubkeyCache keyCache;

	public JWTHelper(JWTServerProperties preferences, IContainerSecurityConfiguration securityProperties, PubkeyCache keyCache){
		this.preferences = preferences;
		this.securityProperties = securityProperties;
		this.serverDN = securityProperties.getCredential()!=null?
				securityProperties.getCredential().getSubjectName() : Client.ANONYMOUS_CLIENT_DN;
		this.keyCache = keyCache;
		loadLocalTrustedIssuers();
	}

	public String createETDToken(String user) throws Exception {
		return createETDToken(user,preferences.getTokenValidity());
	}

	public String createETDToken(String user, long lifetime) throws Exception {
		if(preferences.getHMACSecret()!=null){
			return JWTUtils.createETDToken(user, lifetime, serverDN, 
					preferences.getHMACSecret());
		}
		else {
			return JWTUtils.createETDToken(user, lifetime, serverDN, 
					securityProperties.getCredential().getKey());
		}
	}

	public void verifyJWTToken(String token, String requiredAudience) throws Exception {
		if(preferences.getHMACSecret()!=null && JWTUtils.isHMAC(token)) {
			try{
				JWTUtils.verifyJWTToken(token, preferences.getHMACSecret(), requiredAudience);
				return;
			}catch(Exception ex) {}
		}
		String issuer = JWTUtils.getIssuer(token);
		Collection<PublicKey> pks = keyCache.getPublicKeys(issuer);
		if(pks==null || pks.size()==0) {
			throw new AuthenticationException("Cannot verify token issued by <"+issuer+">, no certificates available.");
		}
		JWTUtils.verifyJWTToken(token, pks, requiredAudience);
	}

	private void loadLocalTrustedIssuers() {
		if(keyCache==null)return;
		List<String> localTrusted = preferences.getListOfValues(JWTServerProperties.TRUSTED_ISSUER_CERT_LOCATIONS);
		for(String location: localTrusted) {
			if(new File(location).exists()) {
				try(InputStream is = new FileInputStream(location)){
					X509Certificate cert = CertificateUtils.loadCertificate(is, Encoding.PEM);
					String dn = cert.getSubjectX500Principal().getName();
					boolean accepted = keyCache.update(cert);
					if(accepted) {
						logger.info("Loaded trusted JWT issuer certificate <{}> from '{}'", dn, location);
					}else {
						logger.debug("Ignoring expired certificate '{}'", location);
					}
				}catch(Exception e) {
					Log.logException("Could not load trusted JWT issuer from '"+location+"'", e, logger);
				}
			}
		}
	}
}