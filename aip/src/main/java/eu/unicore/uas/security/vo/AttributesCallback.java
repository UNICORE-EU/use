/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Feb 24, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import de.fzj.unicore.wsrflite.security.util.AttributeHandlingCallback;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.security.SecurityTokens;
import eu.unicore.uas.security.vo.conf.IBaseVOConfiguration;
import eu.unicore.util.Log;

/**
 * Puts SAML attributes (in UVOS Attribute form(at)) into Client. Rather not used anywhere
 * but just in case. 
 * @author K. Benedyczak
 */
public class AttributesCallback implements AttributeHandlingCallback
{
	private static final Logger logger = Log.getLogger(IBaseVOConfiguration.LOG_PFX, AttributesCallback.class);
	
	private String id;
	private String name;
	
	public AttributesCallback(String id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributesCallback other = (AttributesCallback) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> extractAttributes(SecurityTokens tokens)
	{
		NDC.push(name);
		try
		{
			logger.debug("extractAttributes called");
			Map<String, Object> ctx = tokens.getContext();
			if (ctx == null)
				return null;
			Map<String, String> ret = new HashMap<String, String>();
			HashMap<String, List<ParsedAttribute>> authzAttribsMap = 
					(HashMap<String, List<ParsedAttribute>>) ctx.get(id);
			if (authzAttribsMap == null)
				return null;
			logger.debug("Found attributes in SecurityToken, extracting them " +
					"to be inserted into Client's attributes");
			// TODO this is probably useless, but we cannot store arbitrary
			// objects, it would mess up persistence
			ret.put(id, authzAttribsMap.toString());
			return ret;
		} finally
		{
			NDC.pop();
		}
	}
}
