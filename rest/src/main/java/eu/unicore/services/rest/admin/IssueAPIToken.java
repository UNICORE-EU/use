package eu.unicore.services.rest.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.util.Log;

/**
 * Allows to issue an API token
 *
 * @author schuller
 */
public class IssueAPIToken implements AdminAction {

	@Override
	public String getName() {
		return "IssueAPIToken";
	}

	@Override
	public String getDescription() {
		return "parameters: subject, [lifetime, renewable, preferences] ";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		try {
			String subject = params.remove("subject");
			if(subject==null) {
				throw new IllegalArgumentException("Parameter 'subject' is required.");
			}
			String lifetimeParam = params.remove("lifetime");
			String renewable = params.remove("renewable");
			String preferences = params.remove("preferences");
			if(params.size()>0)throw new IllegalArgumentException("Unknown parameter(s): "+params.keySet());
			Map<String,String> claims = new HashMap<>();
			claims.put("etd", "true");
			JWTServerProperties jwtProps = new JWTServerProperties(kernel.getContainerProperties().getRawProperties());
			X509Credential issuerCred =  kernel.getContainerSecurityConfiguration().getCredential();
			long lifetime = lifetimeParam!=null? Long.valueOf(lifetimeParam): jwtProps.getTokenValidity();
			// make sure it's not longer than the remaining credential lifetime
			Date notAfter = issuerCred.getCertificate().getNotAfter();
			long remainingCredentialLifetime = Math.max(0, notAfter.getTime() - System.currentTimeMillis());
			// if user requested a longer lifetime than is possible, we should fault
			if(lifetimeParam!=null && lifetime>remainingCredentialLifetime) {
				throw new IllegalArgumentException("Requested token lifetime is longer than "
						+ "the remaining server certificate validity.");
			}
			lifetime = Math.min(lifetime, remainingCredentialLifetime);
			if(Boolean.parseBoolean(renewable)) {
				claims.put("renewable", "true");
			}
			if(preferences!=null) {
				checkPrefs(preferences);
				claims.put("preferences", preferences);
			}
			AdminActionResult res = new AdminActionResult(true, "OK");
			res.addResult("token", JWTUtils.createJWTToken(subject, lifetime,
					issuerCred.getSubjectName(), issuerCred.getKey(), claims));
			return res;
		}catch(Exception e) {
			return new AdminActionResult(false, Log.getDetailMessage(e));
		}
	}

	private void checkPrefs(String prefs) {
		for(String value: prefs.split(",")){
			String[]tok = value.split(":");
			if(tok.length!=2)throw new IllegalArgumentException("Invalid format for user preference: "+prefs);
		}
	}

}
