/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 22, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile.ScopedStringValue;
import eu.unicore.uas.security.vo.conf.IBaseVOConfiguration;
import eu.unicore.util.Log;


/**
 * Handles extraction of selected SAML attributes which are to be mapped 
 * onto Unicore incarnation attributes.
 * For each incarnation attribute there can be also a second SAML attribute defined for 
 * storing a default value. The base attribute stores a list of valid values. If there
 * is only the base attribute then default and valid lists of values are assumed to be equal. 
 * <p>
 * By default only attributes in the scope (configured at construction time) are used. However
 * user can provide another scope as an argument to the main method. This scope is assumed to be
 * a preferred VO and  in principle should be a subgroup of the main VO.  
 * Attributes in this scope are processed as those in the main scope. Finally only a list
 * of default values is preserved and is saved in a separate list of the result structure.
 * 
 * @author K. Benedyczak
 */
public class UnicoreAttributesHandler
{
	private static final Logger log = Log.getLogger(IBaseVOConfiguration.LOG_PFX, UnicoreAttributesHandler.class);

	/**
	 * Maps SAML attribute names to unicore attribute definitions. Whenever
	 * default saml attribute is also defined then two strings in this mapping points
	 * to the same unicore attribute definition.
	 */
	protected final Map<String, UnicoreAttributeMappingDef> mappings; 
	/**
	 * Maps unicore attribute names to their definitions.
	 */
	protected final Map<String, UnicoreAttributeMappingDef> mappingsUnicore; 
	protected final String uudbScope;
	
	
	public UnicoreAttributesHandler(IBaseVOConfiguration c, UnicoreAttributeMappingDef[] mappings, boolean pushMode) 
	{
		uudbScope = c.getScope();
		this.mappings = new HashMap<String, UnicoreAttributeMappingDef>();
		this.mappingsUnicore = new HashMap<String, UnicoreAttributeMappingDef>();
		for (UnicoreAttributeMappingDef mapping: mappings)
		{
			if (mapping.getSamlName() == null && mapping.getDefSamlName() == null)
				continue;
			if (mapping.isDisabledInPull())
				continue;
			if (mapping.getSamlName() != null)
				this.mappings.put(mapping.getSamlName(), mapping);
			if (mapping.getDefSamlName() != null)
				this.mappings.put(mapping.getDefSamlName(), mapping);
			this.mappingsUnicore.put(mapping.getUnicoreName(), mapping);
		}
	}
	
	public UnicoreIncarnationAttributes extractUnicoreAttributes(List<ParsedAttribute> from, String selectedVo, 
			boolean remove)
	{
		Map<String, List<String>> retValid = new HashMap<String, List<String>>();
		Map<String, List<String>> retDefault = new HashMap<String, List<String>>();
		Map<String, List<String>> retVoValid = new HashMap<String, List<String>>();
		Map<String, List<String>> retVoDefault = new HashMap<String, List<String>>();
		
		for (int i=from.size() - 1; i >= 0; i--)
		{
			ParsedAttribute a = from.get(i);
			UnicoreAttributeMappingDef mapping = mappings.get(a.getName());
			if (mapping == null)
			{
				log.debug(a.getName() + 
						" attribute received but without UNICORE-specific meaning, ignoring.");
				continue;
			}
			if (remove)
				from.remove(i);

			if (mapping.isNonZeroVal() && a.getStringValues().size() == 0)
			{
				log.debug(a.getName() + 
					" attribute (used for UNICORE " + mapping.getUnicoreName()
					+ ") received but without a value, ignoring.");
				continue;
			}

			boolean isDefault = (mapping.getDefSamlName() != null) && mapping.getDefSamlName().equals(a.getName());
			if (a.getStringValues().size() > 1 && isDefault && !mapping.isMultiVal())
				log.warn(a.getName() + " default attribute (used for UNICORE " + 
					mapping.getUnicoreName()
					+ ") received with multiple values (it should have one, will use a random one!).");


			if (a.getDataType().isAssignableFrom(UVOSAttributeProfile.ScopedStringValue.class)) 
			{
				handleLegacyscopedAttribute(a, selectedVo, mapping, retValid, 
						retDefault, retVoValid, retVoDefault);
			} else
			{
				if (mapping.getSamlName() != null && mapping.getSamlName().equals(a.getName())) 
				{
					addValid(a, mapping, retValid, false);
				} else if (mapping.getDefSamlName() != null && mapping.getDefSamlName().equals(a.getName()))
				{
					addDefault(a, mapping, retDefault, false);
				}
			}
		}

		updateValid(retValid, retDefault);
		updateDefault(retValid, retDefault);
		updateDefault(retVoValid, retVoDefault);
		
		UnicoreIncarnationAttributes ret = new UnicoreIncarnationAttributes();
		ret.setFromLists(retValid, retDefault, retVoDefault);
		return ret;
	}


	/**
	 * UVOS attributes contain scope. We handle them here.
	 * @param scopedAttribute
	 * @param selectedVo
	 * @param mapping
	 * @param retValid
	 * @param retDefault
	 * @param retVoValid
	 * @param retVoDefault
	 */
	private void handleLegacyscopedAttribute(ParsedAttribute scopedAttribute, String selectedVo, 
			UnicoreAttributeMappingDef mapping, Map<String, List<String>> retValid,
			Map<String, List<String>> retDefault, Map<String, List<String>> retVoValid,
			Map<String, List<String>> retVoDefault)
	{
		if (scopedAttribute.getObjectValues().size() == 0)
			return;
		ScopedStringValue value = (ScopedStringValue) scopedAttribute.getObjectValues().get(0);
		String scope = value.getScope();
		if (!scopesEqual(scope, uudbScope) && (selectedVo == null || !scopesEqual(scope, selectedVo))) {
			if (log.isDebugEnabled())
				log.debug(scopedAttribute.getName() +	" attribute (used for UNICORE " + 
						mapping.getUnicoreName() + ") received with value in " +
						"unwanted scope, ignoring. The scope is: " +
						scope + " and value is: " + 
						value.getValue());
			return;
		}

		boolean isVo = (selectedVo != null && scopesEqual(scope, selectedVo));

		if (mapping.getSamlName() != null && mapping.getSamlName().equals(scopedAttribute.getName())) 
		{
			if (scope == null || scopesEqual(scope, uudbScope))
				addValid(scopedAttribute, mapping, retValid, false);
			if (isVo) 
				addValid(scopedAttribute, mapping, retVoValid, true);
		} else if (mapping.getDefSamlName() != null && mapping.getDefSamlName().equals(scopedAttribute.getName()))
		{
			if (scope == null || scopesEqual(scope, uudbScope))
				addDefault(scopedAttribute, mapping, retDefault, false);
			if (isVo)
				addDefault(scopedAttribute, mapping, retVoDefault, true);
		}
	}

	
	/**
	 * Compares two scopes.
	 * @param scopeA
	 * @param scopeB
	 * @return true iff are the same.
	 */
	private static boolean scopesEqual(String scopeA, String scopeB)
	{
		if (scopeA == null && scopeB == null)
			return true;
		if (scopeA == null || scopeB == null)
			return false;
		return scopeA.equals(scopeB);
	}
	
	private void addValid(ParsedAttribute a, UnicoreAttributeMappingDef mapping, Map<String, List<String>> ret, 
			boolean isVo)
	{
		if (log.isDebugEnabled())
			log.debug("Found " + a.getName() + " attribute (used as a valid definition for UNICORE " + 
					mapping.getUnicoreName() + 
					(isVo ? " in preferred VO" : "") +
					"). The 1st value is: " + 
					(a.getStringValues().size() > 0 ? a.getStringValues().get(0) : "<NONE>"));
		ret.put(mapping.getUnicoreName(), a.getStringValues());
	}
	
	private void addDefault(ParsedAttribute a, UnicoreAttributeMappingDef mapping, Map<String, List<String>> ret, 
			boolean isVo)
	{
		if (log.isDebugEnabled())
			log.debug("Found " + a.getName() + " attribute (used as default for UNICORE " + 
					mapping.getUnicoreName() + 
					(isVo ? " in preferred VO" : "") +
					"). The 1st value is: " + 
					(a.getStringValues().size() > 0 ? a.getStringValues().get(0) : "<NONE>"));
		if (mapping.isMultiVal())
			ret.put(mapping.getUnicoreName(), a.getStringValues());
		else
		{
			List<String> toAdd = new ArrayList<String>();
			if (a.getStringValues().size() > 0)
				toAdd.add(a.getStringValues().get(0));
			ret.put(mapping.getUnicoreName(), toAdd);
		}
	}
	
	/**
	 * Adds default values to valid (if not present).
	 * @param retValid
	 * @param retDefault
	 */
	private void updateValid(Map<String, List<String>> retValid, Map<String, List<String>> retDefault)
	{
		Iterator<String> defKeys = retDefault.keySet().iterator();
		while (defKeys.hasNext())
		{
			String defAttr = defKeys.next();
			List<String> currentDef = retDefault.get(defAttr);
			List<String> currentValid = retValid.get(defAttr);
			if (currentValid == null)
				retValid.put(defAttr, currentDef);
			else
			{
				for (String defA: currentDef)
					if (!currentValid.contains(defA))
						currentValid.add(defA);
			}
		}
	}


	/**
	 * Creates default value for attribute if only valid values are present for it.
	 * @param retValid
	 * @param retDefault
	 */
	private void updateDefault(Map<String, List<String>> retValid, Map<String, List<String>> retDefault)
	{
		Iterator<String> validKeys = retValid.keySet().iterator();
		while (validKeys.hasNext())
		{
			String validAttr = validKeys.next();
			List<String> currentDef = retDefault.get(validAttr);
			List<String> currentValid = retValid.get(validAttr);
			
			if (currentDef == null && currentValid.size() > 0)
			{
				currentDef = new ArrayList<String>();
				UnicoreAttributeMappingDef mapping = mappingsUnicore.get(validAttr);
				if (mapping.isMultiVal())
				{
					currentDef.addAll(currentValid);
					log.debug("Using all valid values as default values for attribute " + validAttr);
				} else if (mapping.isNonZeroVal())
				{
					String first = currentValid.get(0);
					currentDef.add(first);
					boolean different = false;
					for (int i=1; i<currentValid.size(); i++)
						if (!first.equals(currentValid.get(i)))
						{
							different = true;
							break;
						}
					if (different)
						log.info("Using the first valid attribute value " +
								"as a default value for attribute " + validAttr + 
								", all values are: " + currentValid.toString());
					 else
						log.debug("Using the only one valid attribute value " +
								"as a default value for attribute " + validAttr + 
								", value is: " + first);
				}
				retDefault.put(validAttr, currentDef);
			}
		}
	}
}







