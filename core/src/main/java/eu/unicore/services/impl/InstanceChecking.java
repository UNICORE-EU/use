package eu.unicore.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.util.Log;

/**
 * Run checks on all wsrf instances of a given service
 * 
 * @author schuller
 */
public class InstanceChecking implements Runnable {
	
	private static final Logger logger=Log.getLogger(Log.UNICORE,InstanceChecking.class);

	private final Home home;
	
	//list of uniqueIDs
	protected final List<String> list = Collections.synchronizedList(new ArrayList<>());
	
	protected final List<InstanceChecker> checkers = Collections.synchronizedList(new ArrayList<>());
	
	
	public InstanceChecking(Home home){
		this.home=home;
	}

	public void addAll(Collection<String>instanceIDs){
		list.addAll(instanceIDs);
	}

	public boolean add(String itemId){
		return list.add(itemId);
	}
	
	public boolean remove(String item){
		return list.remove(item);
	}
	
	
	public boolean addChecker(InstanceChecker c){
		return checkers.add(c);
	}
	
	public boolean removeChecker(InstanceChecker c){
		return checkers.remove(c);
	}
	
	/**
	 * checks the condition on each instance, performs some action, 
	 * and removes the instance from the list if it is not valid anymore
	 */
	public void run(){
		logger.trace("Instance Checking running...");
		ArrayList<String> ids = new ArrayList<>();
		ArrayList<String> toRemove = new ArrayList<>();
		ids.addAll(list);
		String serviceName = home.getServiceName();

		for(Iterator<String> i = ids.iterator() ; i.hasNext() ; ){
			String uniqueID=i.next();
			logger.trace("Checking instance {}/{}", serviceName, uniqueID);
			boolean instanceValid = true;
			try{
				for(InstanceChecker ic: checkers){
					if(home.isShuttingDown()){
						return;
					}
					logger.trace("Testing Checker {}", ic.getClass().getName());
					if(ic.check(home,uniqueID)){
						logger.trace("Applying Checker {}", ic.getClass().getName());
						instanceValid = ic.process(home,uniqueID);
					}
				}
				if(!instanceValid){
					toRemove.add(uniqueID);
				}
			}catch(ResourceUnknownException f){
				toRemove.add(uniqueID);
			}
			//catch Throwable here to avoid the checker going down in case
			//of an uncaught exception
			catch(Throwable e){
				logger.warn("Instance {}/{}", serviceName, uniqueID, e);
			}
		}
		list.removeAll(toRemove);	
	}

}
