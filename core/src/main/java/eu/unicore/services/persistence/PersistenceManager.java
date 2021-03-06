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


package eu.unicore.services.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.persist.impl.LockSupport;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

public class PersistenceManager {

	private static final Logger logger=Log.getLogger(Log.PERSISTENCE,PersistenceManager.class);

	private Class<?> persistClass=null;

	private boolean haveInit=false;

	final Map<String,Store> persistMap = new HashMap<String,Store>();

	private final Map<Class<?>,PersistenceSettings> persistenceSettings=Collections.synchronizedMap(new HashMap<Class<?>,PersistenceSettings>());

	private LockSupport lockSupport;

	private final Kernel kernel;
	
	public PersistenceManager(Kernel kernel){
		this.kernel=kernel;
	}
	
	/**
	 * initialise persistence manager: load persistence class configured via 
	 * container propery {@link ContainerProperties#WSRF_PERSIST_CLASSNAME}
	 */
	synchronized void init(){
		if(!haveInit){
			haveInit=true;
			try{
				String clazz=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_PERSIST_CLASSNAME);
				if(clazz.startsWith("de.fzj.unicore.wsrflite")) {
					clazz = clazz.replace("de.fzj.unicore.wsrflite", "eu.unicore.services");
				}
				persistClass=Class.forName(clazz);
				logger.info("Using '"+persistClass.getName()+"' for permanent storage.");
			}
			catch(Exception e){
				String message=Log.createFaultMessage("Error configuring persistence! Please check the configuration " +
				"and the latest documentation.",e);
				throw new ConfigurationException(message, e);
			}			
		}	
	}

	/**
	 * persist an instance and clear its "dirty" status
	 * @param inst
	 */
	public void persist(Resource inst)throws Exception{
		getPersist(inst.getServiceName()).persist(inst);
	}


	/**
	 * gets the {@link Store} instance for the given service
	 * 
	 * @param serviceName - the name of the service
	 * @return the {@link Store} implementation configured for the given service
	 */
	public synchronized Store getPersist(String serviceName){
		if(persistClass==null){
			init();
		}
		Store p=persistMap.get(serviceName);
		if (p==null){
			try {
				p=(Store)persistClass.getConstructor().newInstance();	
				p.init(kernel,serviceName);
				persistMap.put(serviceName,p);
			} catch (Exception e) {
				throw new RuntimeException("Cannot create instance of persistence class",e);
			}
		}
		return p;	
	}


	/**
	 * retrieve the persistence settings for a given service class
	 * @param service - the service class
	 */
	public synchronized PersistenceSettings getPersistenceSettings(Class<?> service){
		PersistenceSettings ps=persistenceSettings.get(service);
		if(ps==null){
			ps=PersistenceSettings.get(service);
			persistenceSettings.put(service, ps);
		}
		return ps;
	}

	/**
	 * removes the {@link Store} instance of a WSRF Web Service from the PersistenceManager.<br>
	 * @param serviceName - the service name
	 */
	public void removePersist(String serviceName) {
		persistMap.remove(serviceName);
	}

	/**
	 * get helper for dealing with locks (a singleton)
	 * @return {@link LockSupport}
	 */
	public synchronized LockSupport getLockSupport(){
		if(lockSupport==null){
			String config=kernel.getPersistenceProperties().getValue(PersistenceProperties.DB_CLUSTER_CONFIG);
			lockSupport=new LockSupport(config,"__wsrflite_internal__");
		}
		return lockSupport;
	}	
}
