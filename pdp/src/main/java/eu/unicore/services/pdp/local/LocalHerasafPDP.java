package eu.unicore.services.pdp.local;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.Logger;
import org.herasaf.xacml.core.api.PDP;
import org.herasaf.xacml.core.context.RequestMarshaller;
import org.herasaf.xacml.core.context.ResponseMarshaller;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.herasaf.xacml.core.context.impl.MissingAttributeDetailType;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.context.impl.ResponseType;
import org.herasaf.xacml.core.context.impl.ResultType;
import org.herasaf.xacml.core.context.impl.StatusDetailType;
import org.herasaf.xacml.core.context.impl.StatusType;
import org.herasaf.xacml.core.converter.PolicyCombiningAlgorithmJAXBTypeAdapter;
import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.simplePDP.OrderedMapBasedSimplePolicyRepository;
import org.herasaf.xacml.core.simplePDP.SimplePDPConfiguration;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;

import eu.unicore.security.Client;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.pdp.request.creator.HerasafXacml2RequestCreator;
import eu.unicore.services.pdp.request.profile.UnicoreInternalProfile;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;


/**
 * HerasAF based implementation of a local XACML PDP. 
 * 
 * TODO To eliminate logged warning write custom PDP replacing SimplePDP, custom PolicyStore, use PIP. 
 * Otherwise not really needed. 
 * @author golbi
 */
public class LocalHerasafPDP implements UnicoreXPDP, PolicyListener, ISubSystem
{

	private static final Logger log = Log.getLogger(Log.SECURITY, LocalHerasafPDP.class);
	private PDP engine;
	protected HerasafXacml2RequestCreator requestMaker;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	LocalPolicyStore lps = null;
	private String msg = "OK";

	@Override
	public void setKernel(Kernel k) {
		reloadConfig(k);
	}

	void initialize(String configuration, String baseUrl) {
		requestMaker=new HerasafXacml2RequestCreator(new UnicoreInternalProfile(baseUrl));
		try{
			if(lps==null) {
				lps = new LocalPolicyStore(this, configuration);
			}
			else {
				lps.reload(configuration);
			}
		}catch(Exception e) {
			throw new ConfigurationException("", e);
		}
		this.msg = "["+configuration+"]";
	}

	@Override
	public void updateConfiguration(List<Evaluatable> policies, String algorithm)
	{
		SimplePDPConfiguration config = new SimplePDPConfiguration();
		PolicyCombiningAlgorithmJAXBTypeAdapter policyCnv = 
			new PolicyCombiningAlgorithmJAXBTypeAdapter();
		OrderedMapBasedSimplePolicyRepository repo = 
			new OrderedMapBasedSimplePolicyRepository();
		repo.deploy(policies);
		config.setRootCombiningAlgorithm(policyCnv.unmarshal(algorithm));
		config.setPolicyRetrievalPoint(repo);
		
		Lock l=lock.writeLock();
		l.lock();
		try{
			engine = SimplePDPFactory.getSimplePDP(config);
		} finally {
			l.unlock();
		}
	}
	
	private ResponseType authorize(RequestType request)
	{
		Lock l=lock.readLock();
		l.lock();
		try{
			return engine.evaluate(request);
		} finally{
			l.unlock();
		}
	}
	
	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action,
			ResourceDescriptor d) throws Exception
	{
		RequestType request = requestMaker.createRequest(c, action, d);	
		if (log.isDebugEnabled())
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			RequestMarshaller.marshal(request, baos);
			log.debug("XACML request:" + baos.toString());
		}
		ResponseType response = authorize(request);
		if (log.isDebugEnabled())
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ResponseMarshaller.marshal(response, baos);
			log.debug("XACML response:" + baos.toString());
		}
		List<ResultType> results = response.getResults();
		if (results.size() != 1)
			throw new Exception("XACML herasAF PDP BUG: got " + results.size() +
				" results after asking about one resource. Should get 1.");
		ResultType result = results.get(0);
		return new PDPResult(getDecision(result), getComment(result));
	}

	private static PDPResult.Decision getDecision(ResultType result)
	{
		if (result.getDecision().equals(DecisionType.DENY))
			return PDPResult.Decision.DENY;
		if (result.getDecision().equals(DecisionType.PERMIT))
			return PDPResult.Decision.PERMIT;
		return PDPResult.Decision.UNCLEAR;
	}
	
	private static String getComment(ResultType result)
	{
		StatusType status = result.getStatus();
		if (status == null)
			return "";
		StringBuilder msg = new StringBuilder();
		if (status.getStatusCode() != null)
		{
			msg.append("Decision status code: [");
			msg.append(status.getStatusCode().getValue()).append("]\n");
		}
		String m = status.getStatusMessage();
		if (m != null)
		{
			msg.append("Message: [").append(m).append("]\n");
		}
		StatusDetailType detail = status.getStatusDetail();
		if (detail != null)
		{
			List<MissingAttributeDetailType> mas = 
				detail.getMissingAttributeDetails();
			if (mas != null)
			{
				msg.append("The following attributes are missing: [");
				for (MissingAttributeDetailType ma: mas)
				{
					msg.append(" ").append(ma.getAttributeId());
				}
				msg.append(" ]");
			}
		}
		return msg.toString().trim();
	}

	@Override
	public String getStatusDescription() {
		return msg;
	}

	@Override
	public String getName() {
		return "XACML Policy Decision Point";
	}

	@Override
	public void reloadConfig(Kernel k) {
		ContainerSecurityProperties sec = k.getContainerSecurityConfiguration();
		String configuration = sec.getPdpConfigurationFile();
		if (configuration == null) {
			throw new ConfigurationException("For " + LocalHerasafPDP.class.getName() + 
					" PDP a configuration file must be defined.");
		}
		ContainerProperties baseSettings = k.getContainerProperties();
		String baseUrl = baseSettings.getContainerURL();
		initialize(configuration, baseUrl);
	}

}
