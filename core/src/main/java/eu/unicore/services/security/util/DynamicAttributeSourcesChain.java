/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package eu.unicore.services.security.util;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.log4j.NDC;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.SubjectAttributesHolder;
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

	private final static Logger logger=Log.getLogger(Log.SECURITY, DynamicAttributeSourcesChain.class);
	
	/**
	 * combines results from all configured attribute sources
	 */
	@Override
	public SubjectAttributesHolder getAttributes(Client client, SubjectAttributesHolder initial)
			throws IOException, AuthorisationException {
		assert started : "This object must be started before use.";
		SubjectAttributesHolder resultMap = new SubjectAttributesHolder(initial.getPreferredVos());
		for (IDynamicAttributeSource a: chain){
			NDC.push(a.getName());
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
				NDC.pop();
			}
		}
		return resultMap;
	}

	protected void initOrder() throws ConfigurationException {
		chain = new ArrayList<IDynamicAttributeSource>();
		names = new ArrayList<String>();
		if (orderString == null) {			
			String nn = name == null ? "" : "." + name;
			throw new ConfigurationException("Configuration inconsistent, " +
					"need to define <" + ContainerSecurityProperties.PROP_DAP_PREFIX + 
					nn + ".order>");
		}
		String[] authzNames=orderString.split(" +");
		
		if (properties == null)
			throw new IllegalStateException("Properties are null. Please set them using setProperties()");
		
		for(String auth: authzNames){
			chain.add(AttributeSourceConfigurator.configureDynamicAttributeSource(auth, 
					ContainerSecurityProperties.PROP_DAP_PREFIX, properties));
			names.add(auth);
		}
	}
}
