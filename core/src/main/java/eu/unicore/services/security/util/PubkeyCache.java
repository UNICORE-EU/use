package eu.unicore.services.security.util;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.services.Kernel;

public class PubkeyCache {

	final Map<String, PublicKey>map = new ConcurrentHashMap<>();
	
	public PublicKey getPublicKey(String subject){
		return map.get(X500NameUtils.getComparableForm(subject));
	}
	
	public void update(String subject, PublicKey pubkey){
		map.put(X500NameUtils.getComparableForm(subject), pubkey);
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
