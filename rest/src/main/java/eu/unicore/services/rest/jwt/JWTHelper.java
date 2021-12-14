package eu.unicore.services.rest.jwt;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.util.Log;

public class JWTHelper {

	private static final Logger logger = Log.getLogger(Log.SECURITY, JWTHelper.class);

	private final JWTServerProperties preferences;
	
	private final IContainerSecurityConfiguration securityProperties;
	
	private final String issuer;
	
	private final PubkeyCache keyCache;
	
	public JWTHelper(JWTServerProperties preferences, IContainerSecurityConfiguration securityProperties, PubkeyCache keyCache){
		this.preferences = preferences;
		this.securityProperties = securityProperties;
		this.issuer = securityProperties.getCredential()!=null?
				securityProperties.getCredential().getSubjectName() : Client.ANONYMOUS_CLIENT_DN;
		this.keyCache = keyCache;
		loadLocalTrustedIssuers();
	}
	
	public JWTHelper(JWTServerProperties preferences, String issuer, PubkeyCache keyCache){
		this.preferences = preferences;
		this.securityProperties = null;
		this.issuer = issuer;
		this.keyCache = keyCache;
		loadLocalTrustedIssuers();
	}
	
	public String createJWTToken(SecurityTokens tokens) throws Exception {
		return createETDToken(tokens.getEffectiveUserName());
	}

	public String createETDToken(String user) throws Exception {
		return createETDToken(user,preferences.getTokenValidity());
	}
	
	public String createETDToken(String user, long lifetime) throws Exception {
		if(preferences.getHMACSecret()!=null){
			return JWTUtils.createETDToken(user, lifetime, issuer, 
					preferences.getHMACSecret());
		}
		else {
			return JWTUtils.createETDToken(user, lifetime, issuer, 
					securityProperties.getCredential().getKey());
		}
	}

	public void verifyJWTToken(String token) throws Exception {
		if(preferences.getHMACSecret()!=null) {
			try{
				JWTUtils.verifyJWTToken(token, preferences.getHMACSecret());
				return;
			}catch(Exception ex) {}
		}
		String issuer = JWTUtils.getIssuer(token);
		PublicKey pk = keyCache.getPublicKey(issuer);
		if(pk==null) throw new AuthenticationException("No public key is available for <"+issuer+">");
		JWTUtils.verifyJWTToken(token, pk);
	}
	
	private void loadLocalTrustedIssuers() {
		List<String> localTrusted = preferences.getListOfValues(JWTServerProperties.TRUSTED_ISSUER_CERT_LOCATIONS);
		for(String location: localTrusted) {
			try(InputStream is = new FileInputStream(location)){
				X509Certificate cert = CertificateUtils.loadCertificate(is, Encoding.PEM);
				String dn = cert.getSubjectX500Principal().getName();
				logger.info("Loading trusted JWT issuer <{}> from '{}'", dn, location);
				keyCache.update(dn, cert.getPublicKey());
			}catch(Exception e) {
				Log.logException("Could not load trusted JWT issuer from '"+location+"'", e);
			}
		}
	}
}
