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

	private static ThreadLocal<Client> client = new ThreadLocal<>();

	private static ThreadLocal<TimeProfile> timeProfile = new ThreadLocal<>();

	public static Client getClient(){
		Client ret = client.get();
		if (ret == null) {
			ret = new Client();
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
		timeProfile.remove();
	}
}
