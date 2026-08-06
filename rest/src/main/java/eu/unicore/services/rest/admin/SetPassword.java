package eu.unicore.services.rest.admin;

import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.rest.security.AuthenticatorChain;
import eu.unicore.services.rest.security.FilebasedAuthenticator;
import eu.unicore.services.rest.security.IAuthenticator;
import eu.unicore.util.Log;

/**
 * Allows add/update an entry in the user auth file
 * (when using the FilebasedAuthenticator)
 *
 * @author schuller
 */
public class SetPassword implements AdminAction {

	@Override
	public String getName() {
		return "SetPassword";
	}

	@Override
	public String getDescription() {
		return "parameters: username, password, [dn] ";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		try {
			String username = params.remove("username");
			if(username==null) {
				throw new IllegalArgumentException("Parameter 'username' is required.");
			}
			String password = params.remove("password");
			if(password==null) {
				throw new IllegalArgumentException("Parameter 'password' is required.");
			}
			String dn = params.remove("dn");
			if(params.size()>0)throw new IllegalArgumentException("Unknown parameter(s): "+params.keySet());
			var authChain = AuthenticatorChain.getAuthenticatorChain(kernel);
			boolean modified = false;
			boolean haveFilebasedAuth = false;
			for(IAuthenticator auth: authChain.getChain()) {
				if(auth instanceof FilebasedAuthenticator) {
					haveFilebasedAuth = true;
					var fAuth = (FilebasedAuthenticator) auth;
					if(dn==null) {
						dn = fAuth.usernamePassword(username, password);
					}
					if(dn!=null) {
						modified = fAuth.set(username, password, dn);
						if(modified)break;
					}
				}
			}
			if(modified) {
				return new AdminActionResult(true, "OK");
			}
			else {
				return new AdminActionResult(false, "Password could not be set"
						+(!haveFilebasedAuth?" (no auth file configured)":""));
			}
		}catch(Exception e) {
			return new AdminActionResult(false, Log.getDetailMessage(e));
		}
	}

}
