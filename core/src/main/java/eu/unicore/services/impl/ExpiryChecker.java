package eu.unicore.services.impl;

import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.util.Log;

/**
 * Checks Resource instance for lifetime expiry.
 * 
 * @author schuller
 */
public class ExpiryChecker implements InstanceChecker {

	private static final Logger logger=Log.getLogger(Log.UNICORE,ExpiryChecker.class);

	public boolean check(Home home, String id)throws Exception {
		Calendar c = home.getTerminationTime(id);
		return c!=null? c.compareTo(Calendar.getInstance())<=0 : false;
	}

	public boolean process(Home home, String id) {
		String serviceName = home.getServiceName();
		logger.trace("Destroying {}/{}", serviceName, id);
		try(Resource i = home.getForUpdate(id)){
			i.destroy();
		}catch(ResourceUnavailableException e){
			// need to try again
			return true;
		}
		catch(Exception e){
			logger.debug("Could not terminate instance <{}:{}>: {}",serviceName, id, e);
		}
		return false;
	}

}
