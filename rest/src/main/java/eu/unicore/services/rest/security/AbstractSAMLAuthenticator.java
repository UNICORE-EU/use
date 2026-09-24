package eu.unicore.services.rest.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.wsutil.samlclient.AuthnResponseAssertions;
import eu.unicore.security.wsutil.samlclient.SAMLAuthnClient;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import jakarta.xml.ws.WebServiceException;

/**
 * Base class for authenticating to Unity via SAML. <br/>
 *
 * The credentials are extracted from the incoming message. Assertions are validated 
 * using the container's configured trusted assertion issuers.
 * Valid assertions are cached for some time, usually the validity period specified
 * in the authentication assertion from Unity. <br/>
 *
 * Identity assignment is from the returned SAML AuthN assertion, other attributes (uid, groups role)
 * can be assigned via MVL scripts.
 * 
 * @author schuller 
 */
public abstract class AbstractSAMLAuthenticator extends BaseRemoteAuthenticator<AuthnResponseAssertions> {

	private static final Logger logger = Log.getLogger(Log.SECURITY,AbstractSAMLAuthenticator.class);

	public void setValidate(boolean validate) {
		// nop
	}

	@Override
	protected void finalizeInit(){
		super.finalizeInit();
		setExternalSystemName("SAML-IdP "+simpleAddress);
	}

	@Override
	protected AuthnResponseAssertions performAuth(DefaultClientConfiguration clientCfg) throws Exception{
		return doAuth(kernel.getContainerProperties().getContainerURL(), clientCfg);
	}

	@Override
	protected long getExpiryTime(AuthnResponseAssertions auth){
		long expires = System.currentTimeMillis() + defaultCacheTime;
		if(auth.getAuthNAssertions().size()>0){
			try{
				expires = auth.getAuthNAssertions().get(0).getNotOnOrAfter().getTime();
			}catch(Exception ex){}
		}
		return expires;
	}

	@Override
	protected String assignIdentity(AuthnResponseAssertions auth, Map<String,Object> attrs){
		if(identityAssign==null && auth.getAuthNAssertions().size()>0){
			return auth.getAuthNAssertions().get(0).getSubjectName();
		}
		else{
			return RESTUtils.evaluateToString(identityAssign, attrs);
		}
	}

	/**
	 * @param targetUrl - the URL of the service to delegate to
	 * @param clientCfg - security settings for making the call to Unity
	 * 
	 * @throws MalformedURLException
	 * @throws SAMLValidationException
	 */
	protected AuthnResponseAssertions doAuth(String targetUrl, 
			DefaultClientConfiguration clientCfg) throws MalformedURLException, SAMLValidationException
	{
		SAMLAuthnClient client = new SAMLAuthnClient(address, clientCfg);
		NameID requester = new NameID(targetUrl, SAMLConstants.NFORMAT_ENTITY);
		try {
			return client.authenticate(SAMLConstants.NFORMAT_DN, requester, targetUrl);
		}catch(WebServiceException we) {
			if(we.getCause()!=null && (we.getCause() instanceof IOException)) {
				notOK(Log.getDetailMessage(we));
			}
			throw we;
		}
	}

	@Override
	protected Map<String, Object> extractAttributes(AuthnResponseAssertions auth) {
		List<AttributeAssertionParser> samlAttributes = auth.getAttributeAssertions();
		if(samlAttributes==null || samlAttributes.size()==0)return null;
		Map<String, Object> attr = new HashMap<>();
		for(AttributeAssertionParser aap: samlAttributes) {
			try {
				List<ParsedAttribute>samlAttr = aap.getAttributes();
				if(samlAttr!=null && samlAttr.size()!=0) {
					for(ParsedAttribute a: samlAttr) {
						attr.put(a.getName(), a.getStringValues());
					}
				}
			}catch(Exception ex) {
				logger.debug("Parse error: {}", ex.getMessage());
			}
		}
		logger.debug("Parsed attributes: {}", attr);
		return attr;
	}

	@Override
	protected String getAuthNMethod() {
		return "UNITY-SAML";
	}
}