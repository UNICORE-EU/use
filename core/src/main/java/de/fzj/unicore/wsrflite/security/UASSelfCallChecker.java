/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2008-12-22
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package de.fzj.unicore.wsrflite.security;

import eu.unicore.security.SelfCallChecker;

public class UASSelfCallChecker implements SelfCallChecker
{
	private final SecurityManager securityManager;
	
	public UASSelfCallChecker(SecurityManager securityManager){
		this.securityManager=securityManager;
	}
	
	public boolean isSelfCall(String consignor)
	{
		return securityManager.isServer(consignor);
	}
}
