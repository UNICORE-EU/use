package eu.unicore.services.security.util;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * IAttributeSource implementation that combines the results from a chain of attribute sources using
 * a configurable combining policy:
 * <ul>
 *  <li>FIRST_APPLICABLE: the first source returning any result is used</li>
 *  <li>FIRST_ACCESSIBLE: the first accessible (i.e. not throwing an exception) source is used</li>
 *  <li>MERGE_LAST_OVERRIDES (default): all results are combined, so that the later 
 *      attribute sources in the chain can override earlier ones</li>
 *  <li>MERGE : all results are combined, and valid attribute values of the same attribute are merged.
 *  Note that in case of default values for incarnation attributes (used if user doesn't request
 *  a particular value) merging is not done, but values are overridden. This is as for those
 *  attributes in nearly all cases multiple values doesn't make sense (user can have one uid,
 *  primary gid, job may be submitted only to one queue).</li>  
 * </ul> 
 * <p>
 * For each incarnation attribute a final value is computed as follow:
 * <ul>
 *  <li> use the value provided by user (if it is valid, if not - fail the request). If user didn't provide a value </li>
 *  <li> use the value from the preferred VO if present, if not </li>
 *  <li> use the default attribute value </li>
 * </ul>
 * @author schuller
 * @author golbi
 */
public class AttributeSourcesChain extends BaseAttributeSourcesChain<IAttributeSource> implements IAttributeSource{

	private final static Logger logger = Log.getLogger(Log.SECURITY, AttributeSourcesChain.class);
	
	public AttributeSourcesChain() {}
	
	public AttributeSourcesChain(Kernel kernel){
		setup(kernel);
	}

	@Override
	protected void setup(Kernel kernel) {
		super.setup(kernel);
		ContainerSecurityProperties csp = kernel.getContainerSecurityConfiguration();
		setCombiningPolicy(csp.getAIPCombiningPolicy());
		orderString = csp.getAIPOrder();
		configure(null, kernel);
	}

	@Override
	public void reloadConfig(Kernel kernel) {
		ContainerSecurityProperties sp = kernel.getContainerSecurityConfiguration();
		if(sp.getAIPDisableRuntimeUpdates()) {
			logger.debug("Dynamic update of attribute sources is disabled, skipping");
		}
		else {
			setup(kernel);
		}
	}

	/**
	 * combines results from all configured attribute sources
	 */
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder initial)
			throws IOException, AuthorisationException {
		SubjectAttributesHolder resultMap = new SubjectAttributesHolder(initial.getPreferredVos());
		for (IAttributeSource a: chain){
			String name = a.getName();
			ThreadContext.push(name);
			try{
				SubjectAttributesHolder current = a.getAttributes(tokens, resultMap);
				if (logger.isDebugEnabled()) {
					logger.debug("Attribute source {} returned the following attributes:\n{}", name, current);
				}
				if (!combiner.combineAttributes(resultMap, current)) {
					logger.debug("Attributes combiner decided to stop processing attribute sources at {}", name);
					break;
				}
			}
			catch(SubsystemUnavailableException e){
				logger.debug("Attribute source <"+name+"> (temporarily) not available.");
			}
			catch(Exception e){
				logger.error(Log.createFaultMessage("Error accessing attribute source <"+name+">", e));
			}
			finally{
				ThreadContext.pop();
			}
		}
		return resultMap;
	}

	@Override
	protected List<IAttributeSource> createChain(Kernel kernel) throws ConfigurationException {
		assert combiner != null;
		var p = super.createChain(ContainerSecurityProperties.PROP_AIP_PREFIX, kernel);
		List<IAttributeSource> newChain = p.getM1();
		List<String> newNames = p.getM2();
		boolean needAAC = true;
		// add AuthAttributesCollector if not already defined via config
		for(IAttributeSourceBase as: newChain) {
			if(as instanceof AuthAttributesCollector) {
				needAAC = false;
				break;
			}
		}
		if(needAAC) {
			if (combiner == MERGE_LAST_OVERRIDES){
				AuthAttributesCollector aac = new AuthAttributesCollector();
				aac.setAutoConfigured();
				newChain.add(0, aac);
				newNames.add(0, "DEFAULT");
			}else {
				newChain.add(new AuthAttributesCollector());
				newNames.add("DEFAULT");
			}
		}
		return newChain;
	}

}
