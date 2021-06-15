/*********************************************************************************
 * Copyright (c) 2006-2007 Forschungszentrum Juelich GmbH 
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

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.security.SecurityManager;

/**
 * Helper class for storing authorization information 
 * 
 * This uses thread-local storage of {@link Client} object from which
 * other data can be retrieved. If Client object was not set, then an anonymous 
 * or local client is automatically created, set and returned. Note that
 * local calls are always properly detected and it may happen that local call
 * has ANONYMOUS client.
 * 
 * @author schuller
 */
public class AuthZAttributeStore {

	private AuthZAttributeStore (){}
	
	private static ThreadLocal<Client> client=new ThreadLocal<Client>();
	
	public static Client getClient(){
		Client ret = client.get();
		if (ret == null) {
			ret = new Client();
			if (SecurityManager.isLocalCall())
				ret.setLocalClient();
			client.set(ret);
		}
		return ret;
	}

	public static void setClient(Client c){
		client.set(c);
	}
	public static void removeClient(){
		client.remove();
	}
	
	public static SecurityTokens getTokens(){
		return getClient().getSecurityTokens();
	}
	
	public static void clear(){
		client.remove();
	}
}
