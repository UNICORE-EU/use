/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 29-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.vo;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile.ScopedStringValue;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.uas.security.vo.conf.IBaseVOConfiguration;
import eu.unicore.util.Log;

/**
 * Extracts attributes from the message context and converts them into {@link XACMLAttribute}s list.
 * @author golbi
 */
public class XACMLAttributesExtractor
{
	private static final Logger logger = Log.getLogger(IBaseVOConfiguration.LOG_PFX, XACMLAttributesExtractor.class);
	
	
	public static List<XACMLAttribute> getSubjectAttributes(List<ParsedAttribute> authzAttribs, String scope)
	{
		logger.trace("getSubjectAttributes called");
		if (authzAttribs == null)
			return null;

		List<XACMLAttribute> ret = new ArrayList<XACMLAttribute>();
		try
		{
			for (ParsedAttribute voAttr: authzAttribs)
				if (hasProperScope(voAttr, scope))
					map2XACMLAttr(ret, voAttr);
		} catch (URISyntaxException e)
		{
			logger.warn("Name or value of attribute received was not a URI" +
					" and it was supposed to be it. Message is: " + e);
			return null;
		}
		return ret;
	}
	
	protected static boolean hasProperScope(ParsedAttribute a, String scope)
	{
		if (a.getObjectValues().size() == 0)
			return true;
		if (!a.getDataType().isAssignableFrom(ScopedStringValue.class))
			return true;
		ScopedStringValue scopedValue = (ScopedStringValue) a.getObjectValues().get(0);
		String s = scopedValue.getScope();
		if (s == null || scope == null || s.equals(scope.toString()))
			return true;
		logger.debug("Ignoring attribute {} as it has unwanted scope: {}", a.getName(), s);
		return false;
	}
	
	//TODO currently only string and URI attribute values are supported
	//all other are ignored with warning. Scoped strings are inserted as
	//normal string attributes. Maybe a custom XACML data type should be created 
	//for them(?) 
	private static void map2XACMLAttr(List<XACMLAttribute> toFill, 
			ParsedAttribute voAttr) throws URISyntaxException
	{
		if (voAttr.getObjectValues().isEmpty())
		{
			logger.debug("Got SAML attribute without any value, not using as a generic XACML attribute, its name is: {}",
					voAttr.getName());
			return;
		}
		if (voAttr.getDataType().isAssignableFrom(String.class))
		{
			for (String value: voAttr.getStringValues())
			{
				logger.debug("Adding XACML string attribute {} with value {}",
						voAttr.getName(), value);
				toFill.add(new XACMLAttribute(voAttr.getName(), value, XACMLAttribute.Type.STRING));
			}
		}
		
		if (!voAttr.getDataType().isAssignableFrom(ScopedStringValue.class))
		{
			logger.debug("Got SAML attribute with unknown type of object value, not using as a generic XACML attribute, it's name is: {} and value class {}",
					voAttr.getName(), voAttr.getDataType());
			return;
		}
		
		ScopedStringValue scopedValue = (ScopedStringValue) voAttr.getObjectValues().get(0);
		String xacmlDT = scopedValue.getXacmlType();
		if (xacmlDT == null)
		{
			logger.debug("Got SAML attribute without XACML DT set - ignoring, it's name is: {}", voAttr.getName());
			return;
		}
		if (xacmlDT.equals(SAMLConstants.XACMLDT_STRING))
		{
			for (String value: voAttr.getStringValues())
			{
				logger.debug("Adding XACML string attribute {} with value {}", 
						voAttr.getName(), value);
				toFill.add(new XACMLAttribute(voAttr.getName(), value, XACMLAttribute.Type.STRING));
			}
		} else if (xacmlDT.equals("http://www.w3.org/2001/XMLSchema#anyURI"))
		{
			for (String value: voAttr.getStringValues())
			{
				logger.debug("Adding XACML anyURI attribute {} with value {}", voAttr.getName(), value);
				toFill.add(new XACMLAttribute(
					voAttr.getName(), value, XACMLAttribute.Type.ANYURI));
			}
		} else if (xacmlDT.equals(SAMLConstants.XACMLDT_SCOPED_STRING))
		{
			for (String value: voAttr.getStringValues())
			{
				String val = value;
				if (scopedValue.getScope() != null && !scopedValue.getScope().equals("/"))
					val = val + "@" + scopedValue.getScope();
				logger.debug("Adding XACML scoped string attribute {} with value {} as a normal string attribute",
						voAttr.getName(), val);
				toFill.add(new XACMLAttribute(
					voAttr.getName(), val, XACMLAttribute.Type.STRING));
			}
		} else
		{
			logger.warn("Got SAML attribute with unsupported XACML DataType: {}. Attribute name is: {}",
					xacmlDT, voAttr.getName());
			return;
		}
	}
}
