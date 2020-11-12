package eu.unicore.services.rest.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.UnixUser;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.rest.security.BaseRemoteAuthenticator.CacheEntry;
import eu.unicore.services.rest.security.PAMAttributeSource.PAMAttributes;
import eu.unicore.util.Log;

/**
 * authenticates using username+password via the local PAM
 * 
 * @author schuller
 */
public class PAMAuthenticator implements IAuthenticator {

	private static final Logger logger = Log.getLogger(Log.SECURITY,PAMAuthenticator.class);

	private final static Collection<String> s = Collections.singletonList("Basic");

	private String dnTemplate = "CN=%s, OU=pam-local-users";

	private final int cacheTime = 30000;
		
	protected final Map<Object,CacheEntry<UnixUser>> cache = new ConcurrentHashMap<>();
	
	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	@Override
	public final boolean authenticate(Message message, SecurityTokens tokens) {
		HTTPAuthNTokens http = CXFUtils.getHTTPCredentials(message);
		if(http == null)return false;

		String username=http.getUserName();
		String password=http.getPasswd();
		String cacheKey = username+":"+password;
		try
		{
			CacheEntry<UnixUser> ce = cache.get(cacheKey);
			boolean cacheHit = ce!=null && !ce.expired();
			UnixUser unixUser = cacheHit ? ce.auth : null;
			if(unixUser==null){
				PAM pam = new PAM("unicore/x");
				unixUser = pam.authenticate(username, password);
				cache.put(cacheKey, new CacheEntry<>(unixUser,cacheTime));
			}
			
			String dn = String.format(dnTemplate, unixUser.getUserName());
			tokens.setUserName(dn);
			tokens.setConsignorTrusted(true);
			storePAMInfo(unixUser, tokens);
			if(logger.isDebugEnabled() && dn!=null){
				logger.debug("Authenticated "+(cacheHit?"(cached) ":"")+"via "+this+": <"+dn+">");
			}
		}catch(Exception ex){
			Log.logException("Error authenticating using PAM", ex, logger);
		}
		return true;
	}
	
	// store PAM attributes for PAMAttributeSource to pick up
	private void storePAMInfo(UnixUser unixUser, SecurityTokens tokens){
		PAMAttributes attr = new PAMAttributes();
		attr.uid = unixUser.getUserName();
		attr.groups = unixUser.getGroups().toArray(new String[unixUser.getGroups().size()]);
		tokens.getContext().put(PAMAttributeSource.PAM_ATTRIBUTES, attr);
	}

	public void setDNTemplate(String dnTemplate){
		this.dnTemplate = dnTemplate;
	}
	
	public String toString(){
		return "PAM";
	}

}
