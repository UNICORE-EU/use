package eu.unicore.services.aip.saml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.aip.saml.conf.IPullConfiguration;
import eu.unicore.services.aip.saml.conf.PropertiesBasedConfiguration;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Pull user attributes via SAML
 *  
 * @author K. Benedyczak
 */
public class SAMLAttributeSource implements IAttributeSource, ExternalSystemConnector
{
	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, SAMLAttributeSource.class);

	private PropertiesBasedConfiguration conf;
	UnicoreAttributesHandler specialAttrsHandler;
	private String configFile;
	private String name;
	private Kernel kernel;

	private SAMLAttributeFetcher fetcher;

	private Status status = Status.UNKNOWN;

	private String statusMessage = "N/A";

	private final CircuitBreaker cb = new CircuitBreaker();

	@Override
	public void configure(String name, Kernel kernel) throws ConfigurationException {
		initConfig(log, name);
		this.kernel=kernel;
		IClientConfiguration cc = kernel.getClientConfiguration();
		if(cc instanceof DefaultClientConfiguration &&
				conf.getValue(PropertiesBasedConfiguration.CFG_SERVER_USERNAME)!=null){
			DefaultClientConfiguration dcc = (DefaultClientConfiguration)cc;
			dcc.setHttpAuthn(true);
			dcc.setHttpUser(conf.getValue(PropertiesBasedConfiguration.CFG_SERVER_USERNAME));
			dcc.setHttpPassword(conf.getValue(PropertiesBasedConfiguration.CFG_SERVER_PASSWORD));
			log.debug("Authenticating to SAML attribute server with username/password.");
		}
		fetcher = new SAMLAttributeFetcher(conf, cc);
		initFinal(log, SAMLAttributeFetcher.ALL_PULLED_ATTRS_KEY, false);
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo)
					throws IOException
	{
		if(!cb.isOK())
			throw new SubsystemUnavailableException("Attribute source "+name+" is temporarily unavailable");
		try {
			fetcher.fetchAttributes(tokens);
		}catch(Exception sve) {
			cb.notOK();
			throw new IOException(sve);
		}
		@SuppressWarnings("unchecked")
		Map<String, List<ParsedAttribute>> allAttributes = (Map<String, List<ParsedAttribute>>) 
		tokens.getContext().get(SAMLAttributeFetcher.ALL_PULLED_ATTRS_KEY);
		List<ParsedAttribute> serviceAttributesOrig = allAttributes.get(conf.getAttributeQueryServiceURL());
		List<ParsedAttribute> serviceAttributes = new ArrayList<>();
		if (serviceAttributesOrig != null) {
			serviceAttributes.addAll(serviceAttributesOrig);
		}
		return assembleAttributesHolder(serviceAttributes, otherAuthoriserInfo, conf.isPulledGenericAttributesEnabled());
	}

	private void checkConnection() {
		final SecurityTokens st = new SecurityTokens();
		st.setUserName(Client.ANONYMOUS_CLIENT_DN);
		st.setConsignorTrusted(true);
		ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
		Callable<String> check = ()->{
			try {
				fetcher.fetchAttributes(st);
			}catch(SAMLErrorResponseException sre) {}
			catch(Exception e) {
				return Log.createFaultMessage("ERROR", e);
			}
			return "OK";
		};
		try {
			String result = TimeoutRunner.compute(check, ts, 3000);
			if ("OK".equals(result)) {
				statusMessage = "OK [" + name
						+ " connected to " + fetcher.getServerURL() + "]";
				status = Status.OK;
				cb.OK();
			}
			else{
				statusMessage = "CAN'T CONNECT" + " ["+(result!=null ? result : "")+"]";
				status = Status.DOWN;
				cb.notOK();
			}
		}catch(Exception e) {
			statusMessage = Log.createFaultMessage("ERROR checking status",e);
			status = Status.UNKNOWN;
		}
	}

	@Override
	public String getConnectionStatusMessage(){
		checkConnection();
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus(){
		checkConnection();
		return status;
	}

	@Override
	public String getExternalSystemName(){
		return name +" Attribute Source " + fetcher.getSimpleAddress();
	}

	private void initConfig(Logger log, String name)
	{
		this.name = name;
		try
		{
			conf = new PropertiesBasedConfiguration(configFile);
		} catch (IOException e)
		{
			throw new ConfigurationException("Can't read configuration of the SAML subsystem", e);
		}
	}

	private void initFinal(Logger log, String key, boolean pushMode)
	{
		UnicoreAttributeMappingDef[] initializedMappings = Utils.fillMappings(
				conf.getSourceProperties(), Utils.mappings, log);
		log.debug("{}",()->Utils.createMappingsDesc(initializedMappings));
		specialAttrsHandler = new UnicoreAttributesHandler(conf, initializedMappings, pushMode);
	}

	private SubjectAttributesHolder assembleAttributesHolder(List<ParsedAttribute> serviceAttributes,
			SubjectAttributesHolder otherAuthoriserInfo, boolean addGeneric)
	{
		SubjectAttributesHolder ret = new SubjectAttributesHolder();
		String preferredScope = null;
		if(otherAuthoriserInfo!=null) {
			ret.setPreferredVos(null);
			preferredScope = Utils.handlePreferredVo(otherAuthoriserInfo.getPreferredVos(), 
					conf.getScope(), otherAuthoriserInfo.getSelectedVo());
		}
		UnicoreIncarnationAttributes uia = specialAttrsHandler.extractUnicoreAttributes(
				serviceAttributes, preferredScope, true);

		if (addGeneric)
		{		
			List<XACMLAttribute> xacmlAttributes = getSubjectAttributes(serviceAttributes, conf.getScope());
			if (xacmlAttributes != null)
				ret.setXacmlAttributes(xacmlAttributes);
		}
		if (uia.getDefaultAttributes() != null && uia.getValidAttributes() != null) {
			ret.setAllIncarnationAttributes(uia.getDefaultAttributes(), uia.getValidAttributes());
		}

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

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String toString() {
		return getName()+" "+fetcher.getSimpleAddress();
	}

	private List<XACMLAttribute> getSubjectAttributes(List<ParsedAttribute> authzAttribs, String scope)
	{
		List<XACMLAttribute> ret = null;
		if (authzAttribs != null) {
			ret = new ArrayList<>();
			for (ParsedAttribute voAttr: authzAttribs) {
				try {
					map2XACMLAttr(ret, voAttr);
				} catch (URISyntaxException e){}
			}
		}
		return ret;
	}

	// only string and URI attribute values are supported, others are ignored
	private void map2XACMLAttr(List<XACMLAttribute> toFill, 
			ParsedAttribute voAttr) throws URISyntaxException
	{
		if (!voAttr.getObjectValues().isEmpty() && voAttr.getDataType().isAssignableFrom(String.class))
		{
			for (String value: voAttr.getStringValues())
			{
				log.debug("Adding XACML string attribute {} with value {}", voAttr.getName(), value);
				toFill.add(new XACMLAttribute(voAttr.getName(), value, XACMLAttribute.Type.STRING));
			}
		}
	}
}
