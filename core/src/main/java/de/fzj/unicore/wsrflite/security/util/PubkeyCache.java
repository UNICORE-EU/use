package de.fzj.unicore.wsrflite.security.util;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.fzj.unicore.wsrflite.Kernel;
import eu.emi.security.authn.x509.impl.X500NameUtils;

public class PubkeyCache {

	final Map<String, PublicKey>map = new ConcurrentHashMap<>();
	
	public PublicKey getPublicKey(String subject){
		if(subject!=null)subject = X500NameUtils.getComparableForm(subject);
		return map.get(subject);
	}
	
	public void update(String subject, PublicKey pubkey){
		if(subject!=null)subject = X500NameUtils.getComparableForm(subject);
		map.put(subject, pubkey);
	}
	
	public static synchronized PubkeyCache get(Kernel kernel){
		PubkeyCache pc = kernel.getAttribute(PubkeyCache.class);
		if(pc==null){
			pc = new PubkeyCache();
			kernel.setAttribute(PubkeyCache.class, pc);
		}
		return pc;
	}
}
