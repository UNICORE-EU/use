package eu.unicore.services.security;

import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ETDClientSettings;

/**
 * Callback class that handles additional attributes in the User assertion.
 * <p>
 * Currently the following user preferences are recognized: uid, gid, 
 * supplementary_gids, role and selected VO.
 * 
 * @author schuller
 * @author golbi
 */
public class UserAttributeCallback implements eu.unicore.security.UserAttributeHandler{
	
	private static final Logger logger = Log.getLogger(Log.SECURITY,UserAttributeCallback.class);
	
	/**
	 * process an attribute defined in the User assertion
	 * 
	 * @param name -  the name of the attribute
	 * @param nameFormat - the NameFormat
	 * @param values - the array of values
	 * @param mainToken - the security tokens
	 */
	public void processUserDefinedAttribute(String name, String nameFormat, XmlObject[]values, SecurityTokens mainToken){
		if (!nameFormat.equals(ETDClientSettings.SAML_ATTRIBUTE_REQUEST_NAMEFORMAT))
			logger.debug("Ignoring request for unknown attribute of type <{}>", nameFormat);
		
		Map<String, String[]> preferences = mainToken.getUserPreferences();
		if (genericAttributeHandle(IAttributeSource.ATTRIBUTE_XLOGIN, 
				preferences, name, values, false)) 
			return;
		if (genericAttributeHandle(IAttributeSource.ATTRIBUTE_GROUP, 
				preferences, name, values, false)) 
			return;
		if (genericAttributeHandle(IAttributeSource.ATTRIBUTE_ROLE, 
				preferences, name, values, false)) 
			return;
		if (genericAttributeHandle(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, 
				preferences, name, values, true)) 
			return;
		if (genericAttributeHandle(IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS, 
				preferences, name, values, false)) 
			return;
		if (genericAttributeHandle(IAttributeSource.ATTRIBUTE_SELECTED_VO, 
				preferences, name, values, false)) 
			return;
		
		logger.debug("Ignoring request for unknown attribute named <{}>", name);
	}
	
	private boolean genericAttributeHandle(String processedName, Map<String, String[]> preferences, 
			String name, XmlObject[]xmlValues, boolean multivalued) {
		if (!processedName.equals(name))
			return false;
		String []values;
		if (!multivalued) {
			values = new String[1];
			values[0] = Utilities.extractElementTextAsString(xmlValues[0]);
		} else {
			values = new String[xmlValues.length];  
			for (int i=0; i<values.length; i++)
				values[i] = Utilities.extractElementTextAsString(xmlValues[i]);
		}
		preferences.put(processedName, values);
		if(logger.isDebugEnabled()){
			logger.debug("Got request for '{}' with value <{}>", processedName , Arrays.toString(values));
		}
		return true;
	}
}
