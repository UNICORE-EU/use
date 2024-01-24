package eu.unicore.services.aip.saml;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.aip.saml.conf.IBaseConfiguration;
import eu.unicore.util.Log;

/**
 * Extracts attributes from the message context and converts them into {@link XACMLAttribute}s list.
 * @author golbi
 */
public class XACMLAttributesExtractor
{
	private static final Logger logger = Log.getLogger(IBaseConfiguration.LOG_PFX, XACMLAttributesExtractor.class);
	
	
	public static List<XACMLAttribute> getSubjectAttributes(List<ParsedAttribute> authzAttribs, String scope)
	{
		logger.trace("getSubjectAttributes called");
		if (authzAttribs == null)
			return null;

		List<XACMLAttribute> ret = new ArrayList<>();
		try
		{
			for (ParsedAttribute voAttr: authzAttribs)
				map2XACMLAttr(ret, voAttr);
		} catch (URISyntaxException e)
		{
			logger.warn("Name or value of attribute received was not a URI" +
					" and it was supposed to be it. Message is: " + e);
			return null;
		}
		return ret;
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
	}
}
