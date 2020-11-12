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


package de.fzj.unicore.wsrflite.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Resource;
import eu.unicore.util.Log;

/**
 * Base functionality shared by all persistence implementations
 * provides optional load-once semantics
 * 
 * @see de.fzj.unicore.wsrflite.persistence.LoadSemantics
 * 
 * @author schuller
 * @author j.daivandy@fz-juelich.de
 */
public abstract class AbstractStore implements Store {

	private static final Logger logger=Log.getLogger(Log.PERSISTENCE,AbstractStore.class);

	protected String serviceName;

	protected PersistenceSettings settings=PersistenceSettings.getDefaultSettings();

	protected ConcurrentMap<String,Resource> instances;

	protected Kernel kernel;

	public synchronized void init(Kernel kernel, String serviceName) {
		this.kernel=kernel;
		this.serviceName=serviceName;
		instances=new ConcurrentHashMap<String,Resource>();
	}

	public void persist(Resource inst)throws PersistenceException{
		settings=kernel.getPersistenceManager().getPersistenceSettings(inst.getClass());
		if(settings.isLoadOnce())instances.put(inst.getUniqueID(),inst);
		ResourceBean bean=
				new ResourceBean(
						inst.getUniqueID(),
						inst.getServiceName(),
						inst.getClass().getName(),
						inst.getModel());

		_persist(bean);
	}

	public void lock(Resource inst, long timeout, TimeUnit units) throws InterruptedException,PersistenceException, TimeoutException{
		ResourceBean bean=
				new ResourceBean(
						inst.getUniqueID(),
						inst.getServiceName(),
						inst.getClass().getName(),
						null);

		_lock(bean,timeout,units);
	}

	public void unlock(Resource inst) {
		try {
			ResourceBean bean=
					new ResourceBean(
							inst.getUniqueID(),
							inst.getServiceName(),
							inst.getClass().getName(),
							null);

			_unlock(bean);
		} catch (Exception e) {
			String msg=inst!=null?inst.getServiceName()+"["+inst.getUniqueID()+"]":"[null]";
			Log.logException("Error unlocking "+msg,e,logger);
		}
	}

	protected abstract void _lock(ResourceBean dao,long timeout, TimeUnit timeUnit)
			throws InterruptedException,PersistenceException,TimeoutException;
	
	protected abstract void _unlock(ResourceBean bean)throws PersistenceException;

	protected abstract void _persist(ResourceBean bean)throws PersistenceException;

	public Resource read(String uniqueID)throws PersistenceException{
		Resource inst=null;
		if(settings.isLoadOnce()){
			inst=instances.get(uniqueID);
			if(inst!=null){
				inst.setKernel(kernel);
				return inst;
			}
		}
		ResourceBean bean=null;
		try {
			bean = _read(uniqueID);
			if(bean!=null){							
				Class<?> clazz=Class.forName(bean.className);
				inst=(Resource)clazz.getConstructor().newInstance();
				inst.setKernel(kernel);
				inst.setHome(kernel.getHome(serviceName));
				inst.setModel(bean.getState());
				if(settings.isLoadOnce()){
					instances.put(uniqueID,inst);
				}
				bean=null;
			}
		}
		catch (PersistenceException pe){ throw pe; }
		catch (Exception e) {
			throw new PersistenceException( "Error reading instance <"+uniqueID+"> of service '"+serviceName+"'.",e);
		}
		return inst;
	}


	protected abstract ResourceBean _read(String uniqueID) throws PersistenceException;


	public Resource getForUpdate(String uniqueID, long time, TimeUnit timeUnit) throws PersistenceException,TimeoutException{
		Resource inst=null;
		if(settings.isLoadOnce()){
			inst=instances.get(uniqueID);
			if(inst!=null){
				return inst;
			}
		}
		ResourceBean bean=null;
		try {
			bean = _getForUpdate(uniqueID, time, timeUnit);
			if(bean!=null){							
				inst=(Resource)(Class.forName(bean.className).getConstructor().newInstance());
				inst.setKernel(kernel);
				inst.setHome(kernel.getHome(serviceName));
				inst.setModel(bean.getState());
				if(settings.isLoadOnce()){
					instances.put(uniqueID,inst);
				}
				bean=null;
			}
		}
		catch (TimeoutException te){ throw te; }
		catch (PersistenceException pe){ throw pe; }
		catch (Exception e) {
			throw new PersistenceException( "Error reading instance <"+uniqueID+"> of service '"+serviceName+"'.",e);
		}
		return inst;
	}

	protected abstract ResourceBean _getForUpdate(String uniqueID, long time, TimeUnit timeUnit) throws Exception;

	public void remove(String uniqueID)throws PersistenceException{
		_remove(uniqueID);
		if(settings.isLoadOnce())instances.remove(uniqueID);
	}

	protected abstract void _remove(String uniqueID)throws PersistenceException;

	public int size()throws PersistenceException{
		return getUniqueIDs().size();
	}

	public long getCacheHits(){
		return -1;
	}

	public void setPersistenceSettings(PersistenceSettings ps) {
		this.settings=ps;		
	}

	public PersistenceSettings getPersistenceSettings () {
		return settings;
	}

	public String getServiceName(){
		return serviceName;
	}

	/**
	 * get the current statistics for this AbstractStore
	 */
	public Map<String,String>getStatistics(){
		return null;
	}

}
