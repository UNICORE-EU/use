package eu.unicore.services.rest.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.message.Message;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.client.OAuthBearerTokenOutInterceptor;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AttributeHandlingCallback;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * Authenticate to Unity via its SAML interface, using the OAuth Bearer token.
 * 
 * @author schuller 
 */
public class UnityOAuthAuthenticator extends UnityBaseSAMLAuthenticator {

	private final static Collection<String> s = Collections.singletonList("Bearer");
	
	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}
	
	public void setKernel(Kernel kernel){
		super.setKernel(kernel);
		kernel.getSecurityManager().addCallback(getCallback());
	}
	
	public String toString(){
		return "Unity with OAuth Bearer token ["+super.toString()+"]";
	}
	
	@Override
	protected Object extractCredentials(DefaultClientConfiguration clientCfg,
			Message message, SecurityTokens tokens) {
		String bearerToken = CXFUtils.getBearerToken(message);
		if(bearerToken == null)return null;
		
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("n/a");
		clientCfg.setHttpUser("n/a");
		clientCfg.getExtraSecurityTokens().put(OAuthBearerTokenOutInterceptor.TOKEN_KEY, bearerToken);
		
		// store token for later use
		tokens.getUserPreferences().put("UC_OAUTH_BEARER_TOKEN", new String[]{bearerToken});
		
		return bearerToken;
	}

	
	private static AttributeHandlingCallback aac;
	
	private synchronized AttributeHandlingCallback getCallback(){
		if(aac==null){
			aac = new BearerAttributeCallback();
		}
		return aac;
	}
	
	private static class BearerAttributeCallback implements AttributeHandlingCallback{
		
		/**
		 * extracts the OAuth Bearer token from the security tokens 
		 * and returns it as an attribute which will be stored in the Client object
		 */
		public Map<String,String> extractAttributes(SecurityTokens tokens) {
			Map<String,String> result = new HashMap<String,String>();
			String[] attr = tokens.getUserPreferences().get("UC_OAUTH_BEARER_TOKEN");
			if(attr!=null && attr.length>0){
				result.put("UC_OAUTH_BEARER_TOKEN" , attr[0]);
			}
			return result;
		}
	}
	
}
