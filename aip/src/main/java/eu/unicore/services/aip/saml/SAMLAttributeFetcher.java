package eu.unicore.services.aip.saml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.services.aip.saml.conf.IPullConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;


/**
 * This class fetches user's attributes from the SAML Attribute Query service, which is
 * statically configured in the configuration. Attributes received are passed for
 * later use as an input of an XACML policy check.
 * @author K. Benedyczak
 */
public class SAMLAttributeFetcher
{
	/**
	 * Prefix of key used in security context to mark value with the list of 
	 * all Attributes received. It is combined with a SAML server URL.
	 */
	public static final String ALL_PULLED_ATTRS_KEY = "SAMLAttributeFetcher_ALLpulledattrs";

	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, SAMLAttributeFetcher.class);

	private final IPullConfiguration pullConfiguration;
	private final IClientConfiguration clientConfiguration;
	private final int cacheTtl;
	private final Cache<String, List<ParsedAttribute>> cache;

	public static final int MAX_ELEMS = 128;

	// SAML service host without full endpoint
	private String simpleAddress;

	public SAMLAttributeFetcher(IPullConfiguration cc, IClientConfiguration secProv) throws Exception
	{
		this.pullConfiguration = cc;
		this.clientConfiguration = secProv;
		this.cacheTtl = pullConfiguration.getCacheTtl();
		this.cache = initCache();
	}

	private Cache<String, List<ParsedAttribute>> initCache()
	{
		if (cacheTtl > 0)
		{
			return CacheBuilder.newBuilder()
					.maximumSize(MAX_ELEMS)
					.expireAfterAccess(cacheTtl, TimeUnit.SECONDS)
					.expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
					.build();
		}
		else return null;
	}

	public String getServerURL() {
		return pullConfiguration.getAttributeQueryServiceURL();
	}

	public synchronized String getSimpleAddress() {
		if(simpleAddress==null) {
			try {
				URL u = new URL(getServerURL());
				simpleAddress = u.getProtocol()+"://"+u.getAuthority();
			}catch(Exception e) {
				this.simpleAddress = getServerURL();
			}
		}
		return simpleAddress;
	}

	/**
	 * Gets attributes for effective user (as returned by tokens parameter).
	 * Attributes are inserted into {@link SecurityTokens} context 
	 * (under a key ALL_PULLED_ATTRS_KEY+serverURL) as a {@link List} 
	 * of {@link ParsedAttribute} objects. 
	 *
	 * @param tokens
	 * @throws IOException communication errors
	 */
	public void fetchAttributes(SecurityTokens tokens) throws SAMLValidationException
	{
		String dn = tokens.getEffectiveUserName();
		if (dn == null)
			throw new IllegalStateException("Can't authorize unknown user!");
		NameID subject = new NameID(dn, SAMLConstants.NFORMAT_DN);
		getAttributes(subject, tokens);
	}
	
	
	/**
	 * This works as follows: first it is checked if another attribute fetcher pulled the 
	 * attributes from our current SAML server. If so then the list is returned.  
	 * Next we check if we have something cached. If not then 
	 * attributes are really pulled (and cached and stored in a context).
	 * @param subject
	 * @param tokens
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void getAttributes(NameID subject, SecurityTokens tokens) throws SAMLValidationException
	{
		String voServerAddress = pullConfiguration.getAttributeQueryServiceURL();
		Map<String, List<ParsedAttribute>> allAttributes = 
				(Map<String, List<ParsedAttribute>>) tokens.getContext().get(ALL_PULLED_ATTRS_KEY);
		if (allAttributes == null)
		{
			allAttributes = new HashMap<>();
			tokens.getContext().put(ALL_PULLED_ATTRS_KEY, allAttributes);
		}
		
		if (allAttributes.get(voServerAddress) != null)
			return;
		List<ParsedAttribute> attrs;
		try
		{
			String comparableSubjectDN = X500NameUtils.getComparableForm(subject.getXBean().getStringValue());
			if (cacheTtl > 0)
			{
				
				List<ParsedAttribute> cachedAttrs = cache.getIfPresent(comparableSubjectDN);
				if (cachedAttrs != null)
				{
					log.debug("Returning cached attributes for {}", subject);
					attrs = new ArrayList<>(cachedAttrs.size());
					attrs.addAll(cachedAttrs);
				} else
				{
					attrs = doRealReceive(subject);
					cachedAttrs = new ArrayList<>(attrs.size());
					cachedAttrs.addAll(attrs);
					cache.put(comparableSubjectDN, cachedAttrs);
				}
			} else {
				attrs = doRealReceive(subject);
			}
		} catch (SAMLServerException e)
		{
			if (SAMLConstants.SubStatus.STATUS2_UNKNOWN_PRINCIPAL.equals(e.getSamlSubErrorId()))
			{
				log.debug("The user {} is not recognized by the VO server",
						X500NameUtils.getReadableForm(subject.getXBean().getStringValue()));
			} else if (SAMLConstants.SubStatus.STATUS2_AUTHN_FAILED.equals(e.getSamlSubErrorId()))
			{
				log.error("Can't authenticate to the SAML " +
					"server as the local server - probably the local server doesn't " +
					"have the read access to the SAML server.");
			}
			throw e;
		}
		if (attrs.size() == 0)
		{
			log.debug("Got empty list of attributes from the SAML service");
		}

		allAttributes.put(voServerAddress, attrs);
	}

	private List<ParsedAttribute> doRealReceive(NameID subject) throws SAMLValidationException 
	{
		SAMLAttributeQueryClient client;
		try
		{
			client = new SAMLAttributeQueryClient(pullConfiguration.getAttributeQueryServiceURL(), 
					clientConfiguration);
		} catch (MalformedURLException e)
		{
			throw new IllegalStateException("Malformed URL while we checked it??", e);
		}
		log.debug("Performing SAML query for attributes of {}",
				() -> X500NameUtils.getReadableForm(subject.getXBean().getStringValue()));
		String samlId = pullConfiguration.getLocalServerURI();
		NameID myID;
		if (samlId != null) {
			myID = new NameID(pullConfiguration.getLocalServerURI(), SAMLConstants.NFORMAT_ENTITY);
		}
		else {
			myID = new NameID(clientConfiguration.getCredential().getSubjectName(), 
					SAMLConstants.NFORMAT_DN);
		}
		return client.getAssertion(subject, myID).getAttributes();
	}
}
