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
 

package eu.unicore.services.messaging;

/**
 * Provides access to the messaging system of USE
 * 
 * @author schuller
 */
public interface IMessaging {

	/**
	 * register a new messaging provider
	 * 
	 * @see IMessagingProvider
	 * @param provider
	 */
	public void registerProvider(IMessagingProvider provider, String id);
	
	/**
	 * gets or creates a messaging channel with the given name
	 * 
	 * @param name -  the name of the messaging queue
	 * @return a {@link IMessagingChannel}
	 */
	public IMessagingChannel getChannel(String name) throws MessagingException;
	
	/**
	 * get a pullpoint for reading messages targeted at the given destination
	 * 
	 * @param destination - the name of the messaging queue
	 * @return a {@link PullPoint}
	 * @throws MessagingException
	 */
	public PullPoint getPullPoint(String destination) throws MessagingException;
	
	/**
	 * checks if unread messages exist for the given destination
	 * 
	 * @param destination
	 * @return <code>true</code> if unread messages exist
	 */
	public boolean hasMessages(String destination)throws MessagingException;
	
}
