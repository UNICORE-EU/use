package eu.unicore.services.aip.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.security.XACMLAttribute.Type;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Code shared by both existing File sources
 * @author K. Benedyczak
 */
public class FileAttributeSource implements IAttributeSource {

	private static final Logger logger = Log.getLogger(Log.SECURITY, FileAttributeSource.class);

	public static final String SPECIAL_XLOGIN = "xlogin";
	public static final String SPECIAL_ROLE = "role";
	public static final String SPECIAL_GROUP = "group";
	public static final String SPECIAL_SUP_GROUPS = "supplementaryGroups";
	public static final String SPECIAL_ADD_OS_GIDS = "addOsGroups";
	public static final String SPECIAL_QUEUE = "queue";

	private String name;

	private long lastChanged;

	private File uudbFile;

	private String format;

	private enum MatchingTypes {STRICT, REGEXP};

	private boolean strictMatching = true;

	private Map<String, List<Attribute>> map;

	@Override
	public void configure(String name, Kernel kernel) throws ConfigurationException {
		this.name = name;
		try (InputStream is = new FileInputStream(uudbFile)){
			lastChanged = uudbFile.lastModified();
			installNewMappings(createParser().parse(is));
		} catch (FileNotFoundException e1) {
			throw new ConfigurationException("The file " + uudbFile + " configured as an " +
					"input of attribute source " + name + " does not exists");
		} catch (IOException e)
		{
			throw new ConfigurationException("Error loading configuration of file attribute source " +
					name + ": " + e.toString(), e);
		}
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException
	{
		parseIfNeeded();
		String subject = X500NameUtils.getComparableForm(tokens.getEffectiveUserName());
		List<Attribute> attrs = searchFor(subject);
		Map<String, String[]> retAll = new HashMap<>();
		Map<String, String[]> retFirst = new HashMap<>();
		List<XACMLAttribute> retXACML = new ArrayList<>();
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

	protected void installNewMappings(Map<String, List<Attribute>> newData)
	{
		map = newData;
		if (strictMatching)
			canonMap();
	}

	private void canonMap()
	{
		Map<String, List<Attribute>> map2 = new HashMap<>();
		for (var it = map.entrySet().iterator(); it.hasNext() ;)
		{
			var e = it.next();
			String key = e.getKey();
			map2.put(X500NameUtils.getComparableForm(key), e.getValue());
		}
		map = map2;
	}

	public void setMatching(String val) {
		MatchingTypes x = MatchingTypes.valueOf(val.toUpperCase());
		strictMatching = (x==MatchingTypes.STRICT);
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setFile(String uudbFile) {
		this.uudbFile = new File(uudbFile);
	}

	public void setFormat(String format) {
		this.format = format;
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
		try(InputStream is = new FileInputStream(uudbFile))
		{
			installNewMappings(createParser().parse(is));
			logger.info("Updated user attributes were loaded from the file <{}>", uudbFile);
		} catch (Exception e)
		{
			logger.error("Attributes list was NOT updated: {}", e.getMessage());
		}
	}

	protected IFileParser createParser() throws IOException {
		if(format==null) {
			format = detectFormat(uudbFile);
		}
		if("JSON".equalsIgnoreCase(format)) {
			return new JSONFileParser();
		}
		else return new XMLFileParser();
	}

	public String detectFormat(File file) throws IOException {
		String f = null;
		try(InputStream is = new FileInputStream(file))
		{
			String content = IOUtils.toString(is, "UTF-8").strip();
			if(content.startsWith("{")) {
				f = "JSON";
			}
			else if(content.startsWith("<")) {
				f = "XML";
			}
			else throw new ConfigurationException("File <"+file+"> is invalid.");
		}
		return f;
	}

	public String toString() {
		return getName()+" ["+uudbFile.getPath()+" ("+format+")]";
	}

}
