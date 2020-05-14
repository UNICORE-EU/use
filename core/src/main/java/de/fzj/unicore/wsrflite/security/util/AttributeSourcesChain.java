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

package de.fzj.unicore.wsrflite.security.util;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import de.fzj.unicore.wsrflite.security.ContainerSecurityProperties;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
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

	private final static Logger logger=Log.getLogger(Log.SECURITY, AttributeSourcesChain.class);
	
	/**
	 * combines results from all configured attribute sources
	 */
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens, SubjectAttributesHolder initial)
			throws IOException, AuthorisationException {
		if (!started)
			throw new IllegalStateException("This object must be started prior to be used.");
		SubjectAttributesHolder resultMap = new SubjectAttributesHolder(initial.getPreferredVos());
		for (IAttributeSource a: chain){
			NDC.push(a.getName());
			try{
				SubjectAttributesHolder current = a.getAttributes(tokens, resultMap);
				if (logger.isDebugEnabled()) {
					logger.debug("Attribute source " + a.getName() + 
							" returned the following attributes:\n" + current);
				}
				if (!combiner.combineAttributes(resultMap, current)) {
					logger.debug("Attributes combiner decided to stop processing of attribute " +
							"sources at " + a.getName() + ".");
					break;
				}
			}
			catch(Exception e){
				Log.logException("Attribute source <"+a.getClass()+"> not available.", e, logger);
			}
			finally{
				NDC.pop();
			}
		}
		return resultMap;
	}

	@Override
	protected void initOrder() throws ConfigurationException {
		chain = new ArrayList<IAttributeSource>();
		names = new ArrayList<String>();
		if (orderString == null) {			
			String nn = name == null ? "" : "." + name;
			throw new ConfigurationException("Configuration inconsistent, " +
					"need to define <" + ContainerSecurityProperties.PROP_AIP_PREFIX + 
					nn + ".order>");
		}
		String[] authzNames=orderString.split(" +");
		
		if (properties == null)
			throw new IllegalStateException("Properties are null. Please set them using setProperties()");
		
		for(String auth: authzNames){
			chain.add(AttributeSourceConfigurator.configureAttributeSource(auth, 
					ContainerSecurityProperties.PROP_AIP_PREFIX, properties));
			names.add(auth);
		}
	}
	
}
