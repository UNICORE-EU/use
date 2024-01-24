package eu.unicore.services.security;

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
