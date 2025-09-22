package eu.unicore.services.impl;

import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.util.Log;

/**
 * Checks Resource instance for lifetime expiry.
 * 
 * @author schuller
 */
public class ExpiryChecker implements InstanceChecker {

	private static final Logger logger=Log.getLogger(Log.UNICORE,ExpiryChecker.class);

	public boolean check(Home home, String id)throws Exception {
		Calendar c=home.getTerminationTime(id);
		if(c==null)return false;
		logger.debug("Checking {}/{} TT={}", home.getServiceName(), id, c.getTime());
		return c.compareTo(Calendar.getInstance())<=0;
	}

	public boolean process(Home home, String id) {
		String serviceName = home.getServiceName();
		logger.debug("Destroying {}/{}", serviceName, id);
		try(Resource i = home.getForUpdate(id)){
			try{
				i.destroy();
			}
			catch(Exception ex){
				Log.logException("Could not perform cleanup for "+serviceName+"<"+id+">",ex,logger);
			}
		}catch(ResourceUnknownException rue){
			Log.logException("Could not find instance "+serviceName+"<"+id+">",rue,logger);
		}catch(ResourceUnavailableException e){
			Log.logException("Could not lock instance "+serviceName+"<"+id+">",e,logger);
			// need to try again
			return true;
		}
		catch(Exception e){
			Log.logException("Could not terminate instance "+serviceName+"<"+id+">",e,logger);
		}
		return false;
	}

}
