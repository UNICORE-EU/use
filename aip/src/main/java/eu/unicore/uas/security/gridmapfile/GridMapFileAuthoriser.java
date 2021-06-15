/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 06-09-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.gridmapfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.OpensslNameUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.utils.FileWatcher;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;


/**
 * Retrieves local login name(s) from a Globus grid-mapfile.
 * Optionally, there is an additional validation against a set of certificates
 * read from a directory. 
 * 
 * @author demuth
 */
public class GridMapFileAuthoriser implements IAttributeSource 
{
	private static final Logger logger = Log.getLogger(Log.SECURITY, GridMapFileAuthoriser.class);

	//map file, its name is set via setFile()
	private File mapFile;

	private String name;

	private String status = "OK";

	private Map<String,List<String>> map;

	@Override
	public void configure(String name) throws ConfigurationException
	{
		this.name = name;
		parse();
	}

	@Override
	public void start(Kernel kernel) throws FileNotFoundException
	{
		FileWatcher modifiedWatcher = new FileWatcher(mapFile, new Runnable(){
			@Override
			public void run() {
				parse();
			}});
		ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
		ts.getScheduledExecutorService().scheduleWithFixedDelay(modifiedWatcher, 2, 2, TimeUnit.MINUTES);
	}
	
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException {

		String subject = OpensslNameUtils.convertFromRfc2253(tokens.getEffectiveUserName(), true);
		subject = OpensslNameUtils.normalize(subject);
		Map<String, String[]> retAll = new HashMap<String, String[]>();
		Map<String, String[]> retFirst = new HashMap<String, String[]>();
		List<XACMLAttribute> retXACML = new ArrayList<XACMLAttribute>();
		List<String> xlogins = map.get(subject);
		if(xlogins != null)
		{
			String[] loginArray = xlogins.toArray(new String[xlogins.size()]);
			retFirst.put(IAttributeSource.ATTRIBUTE_XLOGIN,loginArray);	
			retAll.put(IAttributeSource.ATTRIBUTE_XLOGIN,loginArray);
			retFirst.put(IAttributeSource.ATTRIBUTE_ROLE,new String[]{"user"});	
			retAll.put(IAttributeSource.ATTRIBUTE_ROLE,new String[]{"user"});
		}

		return new SubjectAttributesHolder(retXACML, retFirst, retAll);
	}

	private void parse() throws ConfigurationException
	{
		if (mapFile==null) {
			status = "FAILED";
			throw new ConfigurationException("Config error for gridmap attribute source <"+name+
					">: property 'file' must be set.");
		}
		if(!mapFile.exists())
		{
			status = "FAILED";
			throw new ConfigurationException("Could not parse grid-mapfile as it does not exist. " +
					"Please change it in your attribute source configuration " +
					"(look for properties named uas.security.attributes.*");
		}
		if(mapFile.isDirectory())
		{
			status = "FAILED";
			throw new ConfigurationException("Could not parse grid-mapfile as it is a directory. " +
					"Please change it in your attribute source configuration " +
					"(look for properties named uas.security.attributes.*");
		}
		if(!mapFile.canRead())
		{
			status = "FAILED";
			throw new ConfigurationException("Could not parse grid-mapfile as it cannot be read. " +
					"Please check permissions of the file "+mapFile.getAbsolutePath());
		}

		GridMapFileParser parser = new GridMapFileParser(mapFile);
		map = parser.parse();

		logger.info("User attributes were loaded from the file " + mapFile);
	}



	@Override
	public String getStatusDescription()
	{
		return "File Attribute Source [" + name + "]: " + 
				status + ", using map file " + mapFile.getAbsolutePath();
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setFile(String mapFile) {
		this.mapFile = new File(mapFile);
	}

}

