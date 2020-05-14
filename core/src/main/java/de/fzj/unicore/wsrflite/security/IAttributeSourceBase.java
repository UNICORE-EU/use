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

package de.fzj.unicore.wsrflite.security;

import de.fzj.unicore.wsrflite.ISubSystem;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.util.AttributeSourceConfigurator;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * IAttributeSourceBase provides the interface for UNICORE/X to retrieve authorisation information
 * (attributes) for a particular request from an attribute provider.
 * This is the base interface wich is extended by two actually used: {@link IAttributeSource} and 
 * {@link IDynamicAttributeSource} which are slightly different.
 * 
 * <em>Lifecycle</em>
 * IAttributeSourceBase implementations are created and initialised by the {@link ContainerSecurityProperties},
 * which will create the instance using Class.forName(), set additional parameters, and finally call
 * the init() method. The IAuthoriser will be created only once, and will be kept alive during the
 * lifetime of the server.
 * <p>
 * <em>Parameter injection</em>
 * When creating an IAttributeSource instance, UNICORE/X will set parameters according to the properties
 * defined in the main configuration file (usually <code>uas.config</code>), provided there is a public
 * setter method. For example, if the class has a field <code>setHost(String host)</code>, it
 * will be automatically invoked by UNICORE/X if the configuration has a property 
 * <code>
 * uas.security.attributes.NAME1.Host
 * </code>
 * Currently parameters can be of type String, boolean, or numerical, for details see {@link AttributeSourceConfigurator}
 * <p>
 * 
 * 
 * @author schuller
 * @author golbi
 */
public interface IAttributeSourceBase extends ISubSystem {

	/**
	 * Configures the source. After calling this method it must be ensured that configuration is sane.
	 * It is guaranteed that this method is invoked after all setter injections on the AIP.
	 * 
	 * @param name - the name of the attribute source
	 */
	public void configure(String name) throws ConfigurationException;

	/**
	 * Makes the AIP ready for work if necessary: starts threads etc. 
	 * 
	 * @param kernel - the USE kernel
	 */
	public void start(Kernel kernel)throws Exception;

}
