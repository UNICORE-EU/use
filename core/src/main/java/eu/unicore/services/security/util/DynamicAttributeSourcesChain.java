package eu.unicore.services.security.util;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IDynamicAttributeSource;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * IDynamicAttributeSource implementation that combines the results from a chain of attribute sources using
 * a configurable combining policy. Behaves as {@link AttributeSourcesChain} with cosmetic changes for the DAP API. 
 * 
 * @author golbi
 */
public class DynamicAttributeSourcesChain extends BaseAttributeSourcesChain<IDynamicAttributeSource> 
		implements IDynamicAttributeSource {

	private final static Logger logger = Log.getLogger(Log.SECURITY, DynamicAttributeSourcesChain.class);
	
	public DynamicAttributeSourcesChain() {}
	
	public DynamicAttributeSourcesChain(Kernel kernel){
		setup(kernel);
	}

	@Override
	protected void setup(Kernel kernel) {
		super.setup(kernel);
		ContainerSecurityProperties csp = kernel.getContainerSecurityConfiguration();
		setCombiningPolicy(csp.getDAPCombiningPolicy());
		orderString = csp.getDAPOrder();
		configure(null, kernel);
	}

	@Override
	public void reloadConfig(Kernel kernel) {
		ContainerSecurityProperties sp = kernel.getContainerSecurityConfiguration();
		if(sp.getDAPDisableRuntimeUpdates()) {
			logger.debug("Dynamic update of dynamic attribute sources is disabled, skipping");
		}
		else {
			setup(kernel);
		}
	}

	/**
	 * combines results from all configured attribute sources
	 */
	@Override
	public SubjectAttributesHolder getAttributes(Client client, SubjectAttributesHolder initial)
			throws IOException, AuthorisationException {
		SubjectAttributesHolder resultMap = new SubjectAttributesHolder(initial.getPreferredVos());
		for (IDynamicAttributeSource a: chain){
			ThreadContext.push(a.getName());
			try{
				SubjectAttributesHolder current = a.getAttributes(client, resultMap);
				if (logger.isDebugEnabled()) {
					logger.debug("Dynamic attribute source {} returned the following attributes:\n{}",
							a.getName(), current);
				}
				if (!combiner.combineAttributes(resultMap, current)) {
					logger.debug("Dynamic attributes combiner decided to stop processing attribute sources at {}",
							a.getName());
					break;
				}
			}
			catch(IOException e){
				Log.logException("Dynamic attribute source <"+a.getClass()+"> not available.", e, logger);
			}
			finally{
				ThreadContext.pop();
			}
		}
		return resultMap;
	}

	protected List<IDynamicAttributeSource> createChain(Kernel k) throws ConfigurationException {
		return super.createChain(ContainerSecurityProperties.PROP_DAP_PREFIX, k).getM1();
	}

}
