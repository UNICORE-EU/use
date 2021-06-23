/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 22, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo;

import java.io.IOException;
import java.net.MalformedURLException;
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
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.uas.security.vo.conf.IPullConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;


/**
 * This class fetches user's attributes from the VO service, which is
 * statically configured in the configuration. Attributes received are passed for
 * later use as an input of an XACML policy check.
 * @author K. Benedyczak
 */
public class VOAttributeFetcher
{
	/**
	 * Prefix of key used in security context to mark value with the list of 
	 * all Attributes received. It is combined with a VO server URL.
	 */
	public static final String ALL_PULLED_ATTRS_KEY = "SAMLPullAuthoriser_ALLpulledattrs";
	
	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, VOAttributeFetcher.class);
	
	private final IPullConfiguration pullConfiguration;
	private final IClientConfiguration clientConfiguration;
	private final int cacheTtl;

	private final Cache<String, List<ParsedAttribute>> cache;
	
	public static final int MAX_ELEMS = 128;

	public VOAttributeFetcher(IPullConfiguration cc, IClientConfiguration secProv) throws Exception
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
	
	/**
	 * Gets attributes from the VO service of the effective user (as returned by tokens
	 * parameter). Attributes are inserted into {@link SecurityTokens} context 
	 * (under a key ALL_PULLED_ATTRS_KEY+serverURL) as a {@link List} 
	 * of {@link ParsedAttribute} objects. 
	 *  
	 * @param tokens
	 * @throws IOException
	 */
	public void authorise(SecurityTokens tokens) throws IOException
	{
		String dn = tokens.getEffectiveUserName();
		if (dn == null)
			throw new IllegalStateException("Can't authorize unknown user!");
		NameID subject = new NameID(dn, SAMLConstants.NFORMAT_DN);
		try
		{
			getAttributes(subject, tokens);
		} catch (Exception e)
		{
			throw new IOException("Unable to retrieve attributes from remote SAML service: " + e.toString(), e);
		}
	}
	
	
	/**
	 * This works as follows: first it is checked if another VOFetcher pulled the 
	 * attributes from our current VO server. If so then the list is returned.  
	 * Next we check if we have something cached. If not then 
	 * attributes are really pulled (and cached and stored in a context).
	 * @param subject
	 * @param tokens
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void getAttributes(NameID subject, SecurityTokens tokens) throws Exception
	{
		String voServerAddress = pullConfiguration.getVOServiceURL();
		Map<String, List<ParsedAttribute>> allAttributes = 
				(Map<String, List<ParsedAttribute>>) tokens.getContext().get(ALL_PULLED_ATTRS_KEY);
		if (allAttributes == null)
		{
			allAttributes = new HashMap<String, List<ParsedAttribute>>();
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
					attrs = new ArrayList<ParsedAttribute>(cachedAttrs.size());
					attrs.addAll(cachedAttrs);
				} else
				{
					attrs = doRealReceive(subject);
					cachedAttrs = new ArrayList<ParsedAttribute>(attrs.size());
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
				log.error("Can't authenticate to the VO " +
					"server as the local server - probably the local server doesn't " +
					"have the read access to the VO server.");
			} else
				log.error("SAML error occured during VO server query: " + e);
			throw e;
		} catch (SAMLValidationException e)
		{
			log.error("Problem retrieving attributes from the VO service: " + e.getMessage());
			throw e;
		} 
		
		if (attrs.size() == 0)
		{
			log.debug("Got empty list of attributes from the VO service");
			throw new Exception();
		}

		allAttributes.put(voServerAddress, attrs);
		return;
	}
	
	protected List<ParsedAttribute> doRealReceive(NameID subject) throws SAMLValidationException 
	{
		SAMLAttributeQueryClient client;
		try
		{
			client = new SAMLAttributeQueryClient(pullConfiguration.getVOServiceURL(), 
					clientConfiguration);
		} catch (MalformedURLException e)
		{
			throw new IllegalStateException("Malformed URL while we checked it??", e);
		}
		
		log.debug("Performing SAML query for attributes of {}",
				() -> X500NameUtils.getReadableForm(subject.getXBean().getStringValue()));
		String samlId = pullConfiguration.getServerURI();
		NameID myID;
		if (samlId != null)
			myID = new NameID(pullConfiguration.getServerURI(), SAMLConstants.NFORMAT_ENTITY);
		else
			myID = new NameID(clientConfiguration.getCredential().getSubjectName(), 
					SAMLConstants.NFORMAT_DN);
		AttributeAssertionParser attributeAssertionParser = client.getAssertion(subject, myID);
		attributeAssertionParser.addProfile(new UVOSAttributeProfile());
		return attributeAssertionParser.getAttributes();
	}
}

