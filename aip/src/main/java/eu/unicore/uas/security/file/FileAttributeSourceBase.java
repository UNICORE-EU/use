/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.uas.security.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.security.XACMLAttribute.Type;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Code shared by both existing File sources
 * @author K. Benedyczak
 */
public abstract class FileAttributeSourceBase implements IAttributeSource {
	private Logger logger;
	public static final String SPECIAL_XLOGIN = "xlogin"; 
	public static final String SPECIAL_ROLE = "role"; 
	public static final String SPECIAL_GROUP = "group"; 
	public static final String SPECIAL_SUP_GROUPS = "supplementaryGroups";
	public static final String SPECIAL_ADD_OS_GIDS = "addOsGroups";
	public static final String SPECIAL_QUEUE = "queue";
	
	private String name;

	protected long lastChanged;
	protected String status = "OK";
	
	//config options
	protected File uudbFile = new File("conf", "simpleuudb");
	
	protected FileAttributeSourceBase(Logger logger) {
		this.logger = logger;
	}
	
	
	@Override
	public void configure(String name) throws ConfigurationException {
		this.name = name;
		AttributesFileParser parser;
		try {
			parser = new AttributesFileParser(new FileInputStream(uudbFile));
		} catch (FileNotFoundException e1) {
			throw new ConfigurationException("The file " + uudbFile + " configured as an " +
					"input of attribute source " + name + " does not exists");
		}
		lastChanged = uudbFile.lastModified();
		try
		{
			installNewMappings(parser.parse());
		} catch (IOException e)
		{
			status = e.getMessage();
			throw new ConfigurationException("Error loading configuration of file attribute source " + 
					name + ": " + e.toString(), e);
		}
	}
	
	@Override
	public void start(Kernel kernel) throws Exception {
	}
	
	@Override
	public String getName()
	{
		return name;
	}

	public void setFile(String uudbFile) {
		this.uudbFile = new File(uudbFile);
	}

	protected void putAttributes(List<Attribute> attrs, Map<String, String[]> allIncRet, 
			Map<String, String[]> firstIncRet, List<XACMLAttribute> authzRet)
	{
		for (Attribute a: attrs)
		{
			String name = a.getName();
			boolean isIncarnation = true;
			if (name.equalsIgnoreCase(SPECIAL_XLOGIN))
				name = IAttributeSource.ATTRIBUTE_XLOGIN;
			else if (name.equalsIgnoreCase(SPECIAL_ROLE))
				name = IAttributeSource.ATTRIBUTE_ROLE;
			else if (name.equalsIgnoreCase(SPECIAL_GROUP))
				name = IAttributeSource.ATTRIBUTE_GROUP;
			else if (name.equalsIgnoreCase(SPECIAL_SUP_GROUPS))
				name = IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS;
			else if (name.equalsIgnoreCase(SPECIAL_ADD_OS_GIDS))
				name = IAttributeSource.ATTRIBUTE_ADD_DEFAULT_GROUPS;
			else if (name.equalsIgnoreCase(SPECIAL_QUEUE))
				name = IAttributeSource.ATTRIBUTE_QUEUES;
			else
				isIncarnation = false;

			if (isIncarnation)
			{
				//defaults: for all we take a first value listed, 
				//except of supplementary groups, where we take all.
				if (!name.equals(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS))
				{
					if (a.getValues().size() > 0)
						firstIncRet.put(name, new String[] {a.getValues().get(0)});
					else
						firstIncRet.put(name, new String[] {});
				} else
					firstIncRet.put(name, a.getValues().toArray(
							new String[a.getValues().size()]));

				allIncRet.put(name, a.getValues().toArray(
						new String[a.getValues().size()]));
			} else
			{
				List<String> values = a.getValues();
				for (String value: values)
					authzRet.add(new XACMLAttribute(name, value, Type.STRING));
				if (values.size() == 0)
					throw new ConfigurationException("XACML Authorization attribute '"+ name +
							"' defined without a value (file attribute source " + 
							getName() + ")");
			}
		}
	}
	
	protected synchronized void parseIfNeeded()
	{
		long lastMod = uudbFile.lastModified();
		if (lastMod <= lastChanged)
			return;
		lastChanged = lastMod;
		try
		{
			AttributesFileParser parser = new AttributesFileParser(
					new FileInputStream(uudbFile));
			installNewMappings(parser.parse());
			logger.info("Updated user attributes were loaded from the file " + 
					uudbFile);
		} catch (Exception e)
		{
			logger.error("The updated attributes list is INVALID: " + e.getMessage() + 
					"\nWill continue to use the previously loaded one.");
		}
	}

	protected abstract void installNewMappings(Map<String, List<Attribute>> newData) throws ConfigurationException;
}
