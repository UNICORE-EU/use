/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 25-10-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp.local;

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
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.uas.pdp.argus.ArgusPAP;
import eu.unicore.uas.pdp.request.creator.HerasafXacml2RequestCreator;
import eu.unicore.uas.pdp.request.profile.UnicoreInternalProfile;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.IClientConfiguration;


/**
 * HerasAF based implementation of a local XACML PDP. 
 * 
 * TODO To eliminate logged warning write custom PDP replacing SimplePDP, custom PolicyStore, use PIP. 
 * Otherwise not really needed. 
 * @author golbi
 */
public class LocalHerasafPDP implements UnicoreXPDP, PolicyListener
{
	private static final Logger log = Log.getLogger(Log.SECURITY, LocalHerasafPDP.class);
	private PDP engine;
	protected HerasafXacml2RequestCreator requestMaker;
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	
	
	/**
	 * used by USE when module is used directly
	 */
	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws Exception 
	{
		if (configuration == null)
			throw new ConfigurationException("For " + LocalHerasafPDP.class.getName() + 
					" PDP a configuration file must be defined.");

		String baseUrl = baseSettings.getContainerURL();
		requestMaker=new HerasafXacml2RequestCreator(new UnicoreInternalProfile(baseUrl));
		new LocalPolicyStore(this, configuration, baseSettings.getThreadingServices());
	}
	
	
	/**
	 * used by {@link ArgusPAP} which is wrapping this module
	 * @param requestCreator
	 */
	public void initialize(HerasafXacml2RequestCreator requestCreator)
	{
		requestMaker = requestCreator;
	}
	
	
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
		ResponseType resp;
		l.lock();
		try{
			resp = engine.evaluate(request);
		} finally{
			l.unlock();
		}
		return resp;
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

}
