/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 22, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.client.SAMLAttributePushOutHandler;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.uas.security.vo.conf.IPullConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;


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
	
	private boolean isEnabled;
	
	private IPullConfiguration pullConfiguration;
	private IClientConfiguration clientConfiguration;
	private boolean disableIfPushed;
	private int cacheTtl;

	private CacheManager cacheMan;
	//private static int counterForEhCache = 0;
	private static final String UNSCOPED_CACHE = VOAttributeFetcher.class.getName()
		+ ".unscoped";
	public static final int MAX_ELEMS = 128;

	public VOAttributeFetcher(IPullConfiguration cc, IClientConfiguration secProv) throws Exception
	{
		pullConfiguration = cc;
		this.clientConfiguration = secProv;
		isEnabled = pullConfiguration.isPullEnabled();
		if (!isEnabled)
			return;
		cacheTtl = pullConfiguration.getChacheTtl();
		disableIfPushed = pullConfiguration.disableIfAttributesWerePushed();
		initCache();
	}
	
	private void initCache()
	{
		if (cacheTtl > 0)
		{
			String cacheConfig="<ehcache name=\"__vo_attribute_cache__\">\n" +
					   "<defaultCache maxElementsInMemory=\""+MAX_ELEMS+"\"\n"+
				        "eternal=\"false\"\n"+
				        "timeToIdleSeconds=\""+cacheTtl+"\"\n"+
				        "timeToLiveSeconds=\""+cacheTtl+"\"\n"+
				        "overflowToDisk=\"false\"\n"+
				        "diskPersistent=\"false\"\n"+
				        "diskExpiryThreadIntervalSeconds=\"240\"\n"+
				        "memoryStoreEvictionPolicy=\"LFU\"/>\n"+
				        "</ehcache>";
			
			ByteArrayInputStream bis=new ByteArrayInputStream(cacheConfig.getBytes());
			cacheMan=CacheManager.create(bis);
			
			Cache unscopedCache = new Cache(
					UNSCOPED_CACHE, 
					MAX_ELEMS, MemoryStoreEvictionPolicy.LFU,
					false, null, false, cacheTtl, cacheTtl, false, 240, null);
			cacheMan.addCache(unscopedCache);
		}
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
		if (!isEnabled)
			throw new IOException("The pull SAML authoriser is disabled");
		Map<String, Object> context = tokens.getContext();
		if (disableIfPushed)
		{	 
			List<?> pushedAssertions = (List<?>) context.get(
				SAMLAttributePushOutHandler.PUSHED_ASSERTIONS);
 			if (pushedAssertions != null && pushedAssertions.size() > 0)
			{
				log.debug("Skipping fetching of attributes as client pushed some.");
				throw new IOException("The pull SAML authoriser is skipped as pushed assertions are available");
			}
		}
		
		if (tokens.getEffectiveUserName() == null)
			throw new IllegalStateException("Can't authorize unknown user!");
		String dn = tokens.getEffectiveUserName();
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
				Cache c = cacheMan.getCache(UNSCOPED_CACHE);
				Element e = c.get(comparableSubjectDN);
				if (e != null)
				{
					if (log.isDebugEnabled())
						log.debug("Returning cached attributes for " + subject);
					List<ParsedAttribute> cachedA = (List<ParsedAttribute>) e.getObjectValue();
					attrs = new ArrayList<ParsedAttribute>(cachedA.size());
					attrs.addAll(cachedA);
				} else
				{
					attrs = doRealReceive(subject);
					List<ParsedAttribute> cachedAttrs = new ArrayList<ParsedAttribute>(attrs.size());
					cachedAttrs.addAll(attrs);
					c.put(new Element(comparableSubjectDN, cachedAttrs));
				}
			} else
				attrs = doRealReceive(subject);
		} catch (SAMLServerException e)
		{
			if (SAMLConstants.SubStatus.STATUS2_UNKNOWN_PRINCIPIAL.equals(e.getSamlSubErrorId()))
			{
				log.debug("The user " + X500NameUtils.getReadableForm(subject.getXBean().getStringValue()) 
						+ " is not recognized by the VO server");
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
		
		log.debug("Performing SAML query for attributes of " + X500NameUtils.getReadableForm(
				subject.getXBean().getStringValue()));
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

