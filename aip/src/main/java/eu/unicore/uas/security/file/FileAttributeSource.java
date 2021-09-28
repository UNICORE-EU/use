/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 06-09-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.Log;


/**
 * Retrieves client's attributes from a file. File format is quite simple:
 * <pre>
 * &lt;fileAttributeSource&gt;
 *   &lt;entry key="CN=someDN,C=PL"&gt;
 *     &lt;attribute name="xlogin"&gt;
 *       &lt;value&gt;nobody&lt;/value&gt;
 *       &lt;value&gt;somebody&lt;/value&gt;
 *     &lt;/attribute&gt;
 *     &lt;attribute name="role"&gt;&lt;value&gt;user&lt;/value&gt;&lt;/attribute&gt;
 *   &lt;/entry&gt;
 * &lt;/fileAttributeSource&gt;
 * </pre>
 * You can add arbitrary number of attributes and attribute values.
 * <p>
 * Configuration of this source consist of two entries:
 * <ul>
 * <li>file - the path of the described above file with attributes</li>
 * <li>matching - strict|regexp In strict mode canonical representation of the key is compared 
 * with the canonical representation of the argument. In regexp mode then key is considered a regular expression
 * and argument is matched with it.  </li>
 * </ul>
 * <p>
 * Evaluation is simplistic: the first entry matching the client is used (important when
 * you use wildcards). 
 * <p>
 * The attributes file is automatically refreshed after any change, before subsequent read. If the
 * syntax is wrong then loud message is logged and old version is used.
 * <p>
 * Some attribute names are special: xlogin, role, group, supplementaryGroups, addOsGroups, queue.
 * Attributes with those names (case insensitive)
 * are handled as those special UNICORE attributes (e.g. xlogin is used to provide available local OS 
 * user names for the client).
 * <p>
 * All other attributes are treated as XACML authorization attributes of String type and are
 * passed to the PDP. Such attributes must have at least one value to be processed.
 * 
 * @author golbi
 *
 */
public class FileAttributeSource extends FileAttributeSourceBase implements IAttributeSource 
{
	private static final Logger logger = Log.getLogger(Log.SECURITY, FileAttributeSource.class);
	
	//config options
	private enum MatchingTypes {STRICT, REGEXP};
	private boolean strictMatching = true;
	
	private Map<String, List<Attribute>> map;
	
	public FileAttributeSource() {
		super(logger);
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException
	{
		parseIfNeeded();
		String subject = X500NameUtils.getComparableForm(tokens.getEffectiveUserName());
		List<Attribute> attrs = searchFor(subject);
		Map<String, String[]> retAll = new HashMap<String, String[]>();
		Map<String, String[]> retFirst = new HashMap<String, String[]>();
		List<XACMLAttribute> retXACML = new ArrayList<XACMLAttribute>();
		if (attrs != null)
			putAttributes(attrs, retAll, retFirst, retXACML);
		return new SubjectAttributesHolder(retXACML, retFirst, retAll);
	}
	
	private List<Attribute> searchFor(String name)
	{
		if (strictMatching)
			return map.get(name);
		else
		{
			Iterator<String> keys = map.keySet().iterator();
			while (keys.hasNext())
			{
				String pattern = keys.next();
				Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(name);
				if (m.matches())
					return map.get(pattern);
			}
		}
		return null;
	}
	
	@Override
	protected void installNewMappings(Map<String, List<Attribute>> newData)
	{
		map = newData;
		if (strictMatching)
			canonMap();
	}
	
	private void canonMap()
	{
		Map<String, List<Attribute>> map2 = new HashMap<String, List<Attribute>>();
		Iterator<Entry<String, List<Attribute>>> it = map.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<String, List<Attribute>> e = it.next();
			String key = e.getKey();
			map2.put(X500NameUtils.getComparableForm(key), e.getValue());
		}
		map = map2;
	}

	public void setMatching(String val) {
		if (val.equalsIgnoreCase(MatchingTypes.STRICT.name()))
			strictMatching = true;
		else if (val.equalsIgnoreCase(MatchingTypes.REGEXP.name()))
			strictMatching = false;
		else
			logger.error("Invalid value of the 'matching' configuration option: " + 
					val + ", using default: " + MatchingTypes.STRICT);
	}
}








