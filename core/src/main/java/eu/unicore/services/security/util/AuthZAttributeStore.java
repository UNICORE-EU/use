package eu.unicore.services.security.util;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.utils.TimeProfile;

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

	private static final ThreadLocal<Client> client = new ThreadLocal<>();
	private static final ThreadLocal<SecurityTokens> secTokens = new ThreadLocal<>();
	private static final ThreadLocal<TimeProfile> timeProfile = new ThreadLocal<>();

	public static Client getClient(){
		return client.get();
	}

	public static void setClient(Client c){
		client.set(c);
	}

	public static SecurityTokens getTokens(){
		Client client = getClient();
		return client!=null ? client.getSecurityTokens() : secTokens.get();
	}

	public static void setTokens(SecurityTokens tokens){
		secTokens.set(tokens);
	}

	public static TimeProfile getTimeProfile(){
		TimeProfile ret = timeProfile.get();
		if(ret==null) {
			ret = new TimeProfile(Thread.currentThread().getName());
			timeProfile.set(ret);
		}
		return ret;
	}

	public static void clear(){
		client.remove();
		secTokens.remove();
		timeProfile.remove();
	}
}
