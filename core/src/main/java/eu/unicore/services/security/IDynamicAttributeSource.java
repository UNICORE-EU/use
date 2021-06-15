/*********************************************************************************
 * Copyright (c) 2006-2010 Forschungszentrum Juelich GmbH 
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

package eu.unicore.services.security;

import java.io.IOException;

import eu.unicore.security.Client;
import eu.unicore.security.SubjectAttributesHolder;

/**
 * IAttributeSource provides the interface for UNICORE/X to retrieve authorisation information
 * (attributes) for a particular request from an attribute provider.
 * <p>
 * The dynamic AIPs, called also DAPs (dynamic attribute points), are called after the authorisation step to provide
 * attributes dynamically assigned to the user basing on previously established static attributes. 
 * Such dynamic attributes can be used for incarnation only (e.g. xlogin).
 * 
 * @see IAttributeSource
 * @author schuller
 * @author golbi
 */
public interface IDynamicAttributeSource extends IAttributeSourceBase {

	/**
	 * Retrieves a map of attributes based on the supplied Client object.
	 * 
	 * Since DAPs can be chained, it might be sometimes useful to see attributes returned by 
	 * DAPs that have run previously. This information is supplied in the "otherAuthoriserInfo" map.
	 * 
	 * Attribute sources must not make any authorisation decisions. Thus, no exceptions must be thrown
	 * if no attributes are found. Only IOExceptions should be thrown in case of technical problems 
	 * contacting the actual attribute provider. This is to allow upstream code (i.e. the UNICORE/X 
	 * server) to log the error, or to take any other action (like notify an administrator). 
	 * If no attributes are found, an empty map or <code>null</code> must be returned.
	 * 
	 * @param client contains data established by the static AIPs, which undergone authorisation. The
	 * object state should not be modified by implementations.
	 * @param otherAuthoriserInfo - attributes returned by other DAPs, which may be <code>null</code>
	 * @return subject's dynamic attributes
	 * @throws IOException in case of technical problems
	 */
	public SubjectAttributesHolder getAttributes(final Client client, 
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException;	 
}
