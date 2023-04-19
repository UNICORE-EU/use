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

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.unicore.persist.PersistenceException;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;

/**
 * Persistent storage interface
 *  
 * @author schuller
 * @author daivandy
 */
public interface Store {

	/**
	 * initialise the store
	 * @param kernel The {@link Kernel} instance
	 * @param serviceName The name of the service that is persisted
	 */
	public void init(Kernel kernel, String serviceName) throws Exception;
	
	/**
	 * shutdown the store
	 */
	public void shutdown();
	
	/**
	 * persist the given Resource
	 */
	public void persist(Resource inst) throws Exception;
	
	/**
	 * Retrieve a list of all unique ids. Usually these will be in order as
	 * ordered by the underlying DB
	 */
	public List<String> getUniqueIDs() throws Exception;
	
	/**
	 * Retrieve a list of all unique ids which are tagged with the given tags. 
	 * Usually these will be in order as ordered by the underlying DB
	 */
	public List<String> getTaggedResources(String... tags) throws Exception;
	
	/**
	 * read an instance from storage.
	 * @return <code>null</code> if instance does not exist
	 */
	public Resource read(String uniqueID) throws Exception;
	
	/**
	 * get and lock a live instance from storage.
	 * This is necessary to aquire write permission
	 * @param uniqueID - the id of the instance to read
	 * @param time - the maximum time to wait when aquiring a lock
	 * @param timeUnit - the time units
	 */
	public Resource getForUpdate(String uniqueID, long time, TimeUnit timeUnit)throws TimeoutException, Exception;
	
	/**
	 * get lock for the given instance. This is used to upgrade a read to a write 
	 * operation.
	 * 
	 * @param inst - the resource to lock
	 * @param timeout - the maximum time to wait when acquiring a lock
	 * @param units - the time units
	 * @throws InterruptedException
	 * @throws PersistenceException
	 * @throws TimeoutException
	 */
	public void lock(Resource inst, long timeout, TimeUnit units) throws TimeoutException, Exception;
	
	
	/**
	 * clear lock for the given instance
	 * @param wsrfInstance
	 */
	public void unlock(Resource wsrfInstance);
	
	
	/**
	 * Delete an instance, and cleanup the persistence. 
	 * A held lock will be released and deleted.
	 * @param uniqueID
	 */
	public void remove(String uniqueID) throws Exception;
	
	/**
	 * returns the number of instances in this store
	 */
	public int size() throws Exception;
	
	/**
	 * sets termination time for a Resource
	 * 
	 * @param uniqueID - Resource id
	 * @param c - termination time for this Resource. May be <code>null</code> to indicate infinite lifetime
	 */
	public abstract void setTerminationTime(String uniqueID, Calendar c) throws Exception;
	
	
	/**	 
	 * gets current termination times for all Resources in this store
	 */
	public abstract Map<String,Calendar> getTerminationTimes()throws Exception;
	
	
	/**
	 * Fully deletes persistent data, for example after service undeployment
	 */
	public abstract void purgePersistentData();
	
}
