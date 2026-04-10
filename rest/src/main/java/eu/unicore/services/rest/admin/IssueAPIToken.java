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
		return "parameters: subject, [lifetime, uid] ";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		try {
			String lifetimeParam = params.get("lifetime");
			String subject = params.get("subject");
			String uid = params.get("uid");
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
				return new AdminActionResult(false,
						"Requested token lifetime is longer than the remaining server certificate validity.");
			}
			lifetime = Math.min(lifetime, remainingCredentialLifetime);
			if(uid!=null)claims.put("uid", uid);
			AdminActionResult res = new AdminActionResult(true, "OK");
			res.addResult("token", JWTUtils.createJWTToken(subject, lifetime,
					issuerCred.getSubjectName(), issuerCred.getKey(), claims));
			return res;
		}catch(Exception e) {
			return new AdminActionResult(false, Log.getDetailMessage(e));
		}
	}

}
