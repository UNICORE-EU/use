/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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
 

package eu.unicore.services.impl;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Home;
import eu.unicore.services.exceptions.ResourceUnknownException;

/**
 * instances of this class are used for periodical checks on all the
 * WSRF instances existing in a specific {@link DefaultHome} 
 * 
 * @author schuller
 * @author demuth
 */
public interface InstanceChecker {

	/**
	 * check condition
	 * @param uniqueID - a WSRFInstance id
	 * @throws ResourceUnknownException - the the given id does not correspond to a Resource
	 * @throws PersistenceException  - on database/persistence problems
	 */
	public boolean check(Home home, String uniqueID) throws ResourceUnknownException, PersistenceException;

	/**
	 * perform processing on the instance (in case check() hits)
	 * @param uniqueID - a WSRFInstance id
	 * @return returns <code>true</code> if instance is still valid. If it is 
	 * invalid, <code>false</code> is returned, and the instance will be removed from further checks
	 */
	public boolean process(Home home, String uniqueID) throws ResourceUnknownException;


}