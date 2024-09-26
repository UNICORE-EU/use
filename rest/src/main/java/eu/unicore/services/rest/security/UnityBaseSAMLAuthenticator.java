package eu.unicore.services.rest.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AssertionParser;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.trust.TruststoreBasedSamlTrustChecker;
import eu.unicore.samly2.validators.SSOAuthnAssertionValidator;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.wsutil.samlclient.AuthnResponseAssertions;
import eu.unicore.security.wsutil.samlclient.SAMLAuthnClient;
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
public abstract class UnityBaseSAMLAuthenticator extends BaseRemoteAuthenticator<AuthnResponseAssertions> {

	private static final Logger logger = Log.getLogger(Log.SECURITY,UnityBaseSAMLAuthenticator.class);

	private boolean validate = true;

	public void setValidate(boolean validate) {
		this.validate = validate;
	}

	protected AuthnResponseAssertions performAuth(DefaultClientConfiguration clientCfg) throws Exception{
		AuthnResponseAssertions auth = doAuth(kernel.getContainerProperties().getContainerURL(), clientCfg);
		if(validate)validate(auth);
		return auth;
	}
	
	protected long getExpiryTime(AuthnResponseAssertions auth){
		long expires = 0;
		if(auth.getAuthNAssertions().size()>0){
			try{
				expires = auth.getAuthNAssertions().get(0).getNotOnOrAfter().getTime();
			}catch(Exception ex){}
		}
		if(expires==0){
			// cache for a short time anyway
			expires = System.currentTimeMillis() + defaultCacheTime;
		}
		return expires;
	}
	
	@Override
	protected String assignIdentity(AuthnResponseAssertions auth, Map<String,Object> attrs){
		if(auth.getAuthNAssertions().size()>0){
			if(auth.getAuthNAssertions().size()>1){
				logger.debug("More than one authn assertion found! Will use first one.");
			}
			return auth.getAuthNAssertions().get(0).getSubjectName();
		}
		else{
			logger.debug("No authentication assertion found!");
		}
		return null;
	}

	protected void validate(AuthnResponseAssertions authn){
		X509CertChainValidator x509 = kernel.getContainerSecurityConfiguration().getTrustedAssertionIssuers();
		TruststoreBasedSamlTrustChecker samlTrustChecker = new TruststoreBasedSamlTrustChecker(x509);
		String endpointURI = kernel.getContainerProperties().getContainerURL();
		String consumerName = kernel.getContainerSecurityConfiguration().getCredential().getSubjectName();

		SSOAuthnAssertionValidator validator = new SSOAuthnAssertionValidator(consumerName, 
				endpointURI, null, 0, samlTrustChecker, null, SAMLBindings.OTHER);
		validator.setLaxInResponseToChecking(true);
		validator.addConsumerSamlNameAlias(endpointURI);

		logger.debug("Validating AuthN assertions. endpointURI={} consumerName={}", endpointURI, consumerName);
		
		for(AssertionParser ap : authn.getAuthNAssertions()){
			try{
				logger.debug("Validating {}", ap.getXMLBeanDoc());
				validator.validate(ap.getXMLBeanDoc());
			} catch(Exception e1) {
				logger.warn("SAML authentication assertion is " +
						"not trusted: {}", e1.getMessage());
				throw new AuthenticationException("SAML authentication assertion is " +
						"not trusted: " + e1.getMessage());
			}
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
				cb.notOK();
			}
			throw we;
		}
	}

	@Override
	public String getExternalSystemName(){
		return  "Unity @ "+simpleAddress;
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
