/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.services.security;

import java.io.IOException;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.util.configuration.ConfigurationException;

public class NullAttributeSource implements IAttributeSource, IDynamicAttributeSource {
	public NullAttributeSource(){}
	@Override
	public String getStatusDescription(){ return "(No dynamic attribute source configured)"; }
	@Override
	public String getName() { return "Dynamic attributes"; }
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException		{
		return new SubjectAttributesHolder();
	}
	
	@Override
	public void configure(String name) throws ConfigurationException {
	}
	@Override
	public void start(Kernel kernel) throws Exception {
	}
	@Override
	public SubjectAttributesHolder getAttributes(Client client,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException {
		return new SubjectAttributesHolder();
	}
}