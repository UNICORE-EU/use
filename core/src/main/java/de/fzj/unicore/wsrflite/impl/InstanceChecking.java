/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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
 

package de.fzj.unicore.wsrflite.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import eu.unicore.util.Log;

/**
 * Run checks on all wsrf instances of a given service
 * 
 * @author schuller
 */
public class InstanceChecking implements Runnable {
	
	private static final Logger logger=Log.getLogger(Log.WSRFLITE,InstanceChecking.class);

	private final Home home;
	
	//list of uniqueIDs
	protected final List<String> list = Collections.synchronizedList(new ArrayList<String>());
	
	protected final List<InstanceChecker> checkers = Collections.synchronizedList(new ArrayList<InstanceChecker>());
	
	
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
		if(logger.isTraceEnabled())logger.trace("Instance Checking running...");
		ArrayList<String> ids = new ArrayList<>();
		ArrayList<String> toRemove = new ArrayList<>();
		ids.addAll(list);
		String serviceName = home.getServiceName();

		for(Iterator<String> i = ids.iterator() ; i.hasNext() ; ){
			String uniqueID=i.next();
			if(logger.isTraceEnabled())logger.trace("Checking instance of type "+serviceName+" and id "+uniqueID);
			boolean instanceValid = true;
			try{
				for(InstanceChecker ic: checkers){
					if(home.isShuttingDown()){
						return;
					}
					if(logger.isTraceEnabled())logger.trace("Testing Checker "+ic.getClass().getName());
					if(ic.check(home,uniqueID)){
						if(logger.isTraceEnabled()){
							logger.trace("Applying Checker "+ic.getClass().getName());
						}
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
				logger.warn("Instance of type "+serviceName+" with id <"+uniqueID+">",e);
			}
		}
		list.removeAll(toRemove);	
	}

}
