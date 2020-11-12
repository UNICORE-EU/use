/*
 * Copyright (c) 2009 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2010-04-01
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.unicore.uas.security.vo.conf.PropertiesBasedConfiguration;

/**
 * Misc methods useful for both {@link SAMLPullAuthoriser} and {@link SAMLPushAuthoriser}.
 * @author golbi
 */
public class VOCommonUtils
{
	public static final UnicoreAttributeMappingDef[] mappings = {
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_XLOGIN, false, true),
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_ROLE, false, true),
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_GROUP, false, false),
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, true, false),
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS, false, false),
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_QUEUES, false, true),
		new UnicoreAttributeMappingDef(IAttributeSource.ATTRIBUTE_VOS, true, true)
	};

	public static final String PREFIX = PropertiesBasedConfiguration.PREFIX+
			PropertiesBasedConfiguration.CFG_INCARNATION_ATTR_PFX;
	
	/**
	 * Checks if our scope is parent of any of the preferredVos. If so then the first such preferred 
	 * VO is returned but only if this VO is on the same or higher position on the list as previouslySetVo.
	 * In any other case null is returned. 
	 */
	public static String handlePreferredVo(String[] preferredVos, String ourScope, String previouslySetVo)
	{
		if (preferredVos == null)
			return null;
		for (int i=0; i<preferredVos.length; i++)
		{
			String preferred = preferredVos[i];
			if (preferred.startsWith(ourScope))
				return preferredVos[i];
			
			if (previouslySetVo != null && previouslySetVo.equals(preferredVos[i]))
				return null;
		}
		return null;
	}
	
	public static UnicoreAttributeMappingDef[] fillMappings(Properties conf, UnicoreAttributeMappingDef[] rawList,
			Logger log)
	{
		StringBuilder allKnown = new StringBuilder();
		for (UnicoreAttributeMappingDef mapp: rawList)
			allKnown.append(mapp.getUnicoreName() + " ");
		Map<String, UnicoreAttributeMappingDef> result = new HashMap<String, UnicoreAttributeMappingDef>();
		Iterator<Entry<Object, Object>> all = conf.entrySet().iterator();
		while (all.hasNext())
		{
			Entry<Object, Object> e = all.next();
			String key = (String)e.getKey();
			String val = (String)e.getValue();
			if (!key.startsWith(PREFIX))
				continue;
			String key2 = key.substring(PREFIX.length());
			if (key2.length() == 0)
			{
				log.warn("Invalid configuration option: " + key);
				continue;
			}
			
			String[] elems = key2.split("\\.");
			int i=0;
			for (; i<rawList.length; i++)
				if (rawList[i].getUnicoreName().equalsIgnoreCase(elems[0]))
					break;
			if (i == rawList.length)
			{
				log.warn("Unknown UNICORE attribute '" + elems[0] + 
					"' was used. Known are: " + allKnown);
				continue;
			}
			if (elems.length == 1) //vo.unicoreAttribute.key2=...
			{
				if (val.isEmpty())
				{
					log.warn("Definition of UNICORE attribute '" + elems[0] + 
							"' must have a value (SAML name).");
					continue;
				}
				addMapping(result, rawList[i]).setSamlName(val);
				continue;
			}
			if (elems.length > 2)
			{
				log.warn("Invalid configuration entry: " + key);
				continue;
			}
			if (elems[1].equals("default"))
			{
				if (val.isEmpty())
				{
					log.warn("Definition of a default for UNICORE attribute '" + elems[0] + 
							"' must have a value (SAML name).");
					continue;
				}

				addMapping(result, rawList[i]).setDefSamlName(val);
				continue;
			}
			if (elems[1].equals("disabled"))
			{
				log.info("Attribute " + elems[0] + " won't be used in incarnation.");
				UnicoreAttributeMappingDef mapping = addMapping(result, rawList[i]);
				mapping.setDisabledInPull(true);
				mapping.setDisabledInPush(true);
				continue;
			}
			if (elems[1].equals("pullDisabled"))
			{
				log.info("Pulled attribute " + elems[0] + " won't be used in incarnation.");
				addMapping(result, rawList[i]).setDisabledInPull(true);
				continue;
			}
			if (elems[1].equals("pushDisabled"))
			{
				log.info("Pushed attribute " + elems[0] + " won't be used in incarnation.");
				addMapping(result, rawList[i]).setDisabledInPush(true);
				continue;
			}			
		}
		
		return result.values().toArray(new UnicoreAttributeMappingDef[result.size()]);
	}

	private static UnicoreAttributeMappingDef addMapping(Map<String, UnicoreAttributeMappingDef> result,
			UnicoreAttributeMappingDef raw)
	{
		UnicoreAttributeMappingDef r = result.get(raw.getUnicoreName());
		if (r == null)
		{
			r = new UnicoreAttributeMappingDef(raw.getUnicoreName(),
					raw.isMultiVal(), raw.isNonZeroVal());
			result.put(raw.getUnicoreName(), r);
		}
		return r;
	}
	
	public static String createMappingsDesc(UnicoreAttributeMappingDef []mappings)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<mappings.length; i++)
		{
			sb.append(createMappingDesc(mappings[i]));
			if (i<mappings.length-1)
				sb.append("\n");
		}
		return sb.toString();
	}
	
	public static String createMappingDesc(UnicoreAttributeMappingDef mapping)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("UNICORE attribute mapping: ");
		sb.append(mapping.getUnicoreName());
		sb.append(" <- (");
		if (mapping.getSamlName() != null)
		{
			sb.append("saml valid: ");
			sb.append(mapping.getSamlName());
			sb.append(" ");
		}
		if (mapping.getDefSamlName() != null)
		{
			sb.append("saml default: ");
			sb.append(mapping.getDefSamlName());
		}
		sb.append(")");
		if (mapping.isDisabledInPull())
			sb.append(" [disabled when pulling]");
		if (mapping.isDisabledInPush())
			sb.append(" [disabled for pushed]");
		return sb.toString();
	}
}
