/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package eu.unicore.uas.security.vo;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.uas.security.vo.conf.PropertiesBasedConfiguration;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Contains logic which is common for both pull and push SAML AIPs.
 * @author K. Benedyczak
 */
public abstract class SAMLAttributeSourceBase implements IAttributeSource
{
	protected PropertiesBasedConfiguration conf;
	protected UnicoreAttributesHandler specialAttrsHandler;
	protected String configFile;
	protected String name;
	protected boolean isEnabled;
	protected Kernel kernel;
	
	protected void initConfig(Logger log, String name)
	{
		this.name = name;
		isEnabled = false;

		try
		{
			conf = new PropertiesBasedConfiguration(configFile);
		} catch (IOException e)
		{
			throw new ConfigurationException("Can't read configuration of the VO subsystem: " +
					e.getMessage());
		}
		if(!conf.isPullEnabled()) return;
		
		isEnabled = true;
	}
	
	protected void initFinal(Logger log, String key, boolean pushMode)
	{
		log.info("Adding VO attributes callbacks");
		AttributesCallback callback = new AttributesCallback(key, name);
		kernel.getSecurityManager().addCallback(callback);
		
		UnicoreAttributeMappingDef []initializedMappings = VOCommonUtils.fillMappings(
				conf.getSourceProperties(), VOCommonUtils.mappings, log);
		if (log.isDebugEnabled())
			log.debug(VOCommonUtils.createMappingsDesc(initializedMappings));
		specialAttrsHandler = new UnicoreAttributesHandler(conf, initializedMappings, pushMode);
	}
	
	protected SubjectAttributesHolder assembleAttributesHolder(List<ParsedAttribute> serviceAttributes, 
			SubjectAttributesHolder otherAuthoriserInfo, boolean addGeneric)
	{
		SubjectAttributesHolder ret = new SubjectAttributesHolder(otherAuthoriserInfo.getPreferredVos());
		String preferredScope = VOCommonUtils.handlePreferredVo(otherAuthoriserInfo.getPreferredVos(), 
				conf.getScope(), otherAuthoriserInfo.getSelectedVo());
		List<ParsedAttribute> convertedAttributes = UVOSAttributeProfile.splitByScopes(serviceAttributes);
		UnicoreIncarnationAttributes uia = specialAttrsHandler.extractUnicoreAttributes(
				convertedAttributes, preferredScope, true);
		
		if (addGeneric)
		{		
			List<XACMLAttribute> xacmlAttributes = XACMLAttributesExtractor.getSubjectAttributes(
					convertedAttributes, conf.getScope());
			if (xacmlAttributes != null)
				ret.setXacmlAttributes(xacmlAttributes);
		}
		if (uia.getDefaultAttributes() != null && uia.getValidAttributes() != null)
			ret.setAllIncarnationAttributes(uia.getDefaultAttributes(), uia.getValidAttributes());
		
		//preferred scope is for sure subscope of our scope or our scope. But we are not sure if the 
		// user is really a member of the preferred scope. If not we are not setting the preferred VO at all
		// even as we are sure that the list of attributes is empty (there should be no selected VO at all).
		if (uia.getDefaultVoAttributes() != null && preferredScope != null && ret.getValidIncarnationAttributes() != null) 
		{
			String []usersVos = ret.getValidIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_VOS);
			if (usersVos != null)
			{
				for (String userVo: usersVos)
					if (userVo.equals(preferredScope)) 
					{
						ret.setPreferredVoIncarnationAttributes(preferredScope, 
								uia.getDefaultVoAttributes());
						break;
					}
			}
		}
		return ret;
	}

	public void setConfigurationFile(String configFile)
	{
		this.configFile = configFile;
	}
	
	public String getConfigurationFile()
	{
		return configFile;
	}

	@Override
	public String getName()
	{
		return name;
	}

}
