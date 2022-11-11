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


package eu.unicore.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.exceptions.TerminationTimeChangeRejectedException;
import eu.unicore.services.exceptions.UnableToSetTerminationTimeException;
import eu.unicore.services.messaging.MessagingException;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.persistence.Store;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.SecurityManager;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.utils.LoadingMap;
import eu.unicore.services.utils.Pair;
import eu.unicore.util.Log;

/**
 * This is a basic implementation of the Home interface. 
 * It is responsible for managing Resources of a single service.
 * Furthermore it sets up the periodical check for expired resources. 
 * 
 * @author schuller
 */
public abstract class DefaultHome implements Home {

	protected static final Logger logger = Log.getLogger(Log.UNICORE,DefaultHome.class);    

	protected Store serviceInstances;

	protected String serviceName;

	protected static Map<String,Calendar>terminationTimes = new ConcurrentHashMap<>();

	// guard for updates of the the terminationTimes map
	private final static Object ttLock = new Object();

	protected final Map<String,Pair<String, List<ACLEntry>>> secInfoCache;

	private static long lastAccessed;

	private static long tt_update_interval=10000; //millis

	private Kernel kernel;

	protected final Map<String, AtomicInteger> instancesPerUser=new ConcurrentHashMap<String, AtomicInteger>();

	private volatile boolean isShuttingDown=false;

	/**
	 * this takes care of removing expired WS-Resources etc.
	 */
	protected InstanceChecking instanceChecking;
	protected InstanceChecker expiryChecker;

	//does this service support notification
	protected boolean supportsNotification=false;

	private long lastRefreshNotificationInstant=System.currentTimeMillis();

	// timeout when locking a resource, e.g. in getForUpdate()
	private long locking_timeout=30; //seconds

	public DefaultHome(){
		instanceChecking = new InstanceChecking(this);
		expiryChecker = new ExpiryChecker();
		instanceChecking.addChecker(expiryChecker);
		secInfoCache = new LoadingMap<>(
				new Function<String, Pair<String,List<ACLEntry>>>() {
					public Pair<String,List<ACLEntry>> apply(String id) {
						try{
							return readSecurityInfo(id);				
						}catch(Exception ex) {
							throw new RuntimeException(ex);
						}
					}
				});
	}

	@Override
	public void start(String serviceName)throws Exception{
		if(serviceName==null || "".equals(serviceName)){
			throw new IllegalArgumentException("Must specify a service name.");
		}
		if(kernel==null){
			throw new IllegalStateException("The kernel field has not been initialized");
		}
		this.serviceName=serviceName;
		if(serviceInstances==null){
			serviceInstances=kernel.getPersistenceManager().getPersist(serviceName);
		}
		Collection<String> uniqueIDs=serviceInstances.getUniqueIDs();
		logger.info("[{}] Have {} instances from permanent storage.", serviceName, uniqueIDs.size());
		asyncInit(uniqueIDs);
		initNotification();
		locking_timeout = kernel.getContainerProperties().
				getSubkeyIntValue(ContainerProperties.INSTANCE_LOCKING_TIMEOUT, serviceName);
	}

	protected void asyncInit(Collection<String> uniqueIDs) throws Exception {
		Runnable r = new Runnable() {
			public void run() {
				boolean logIt = true;
				Iterator<String>iter = uniqueIDs.iterator();
				while(iter.hasNext()){
					String id = iter.next();
					try{
						doPerInstanceActivation(id);
					}
					catch(Exception e){
						recoverInstanceActivationError(e, id, logIt);
						logIt = true;			
						iter.remove();
					}
				}
				try{
					initExpiryCheck(uniqueIDs);
				}catch(Exception ex) {
					throw new RuntimeException(ex);
				}
				logger.info("[{}] Initialisation done.", serviceName);
			}
		};
		kernel.getContainerProperties().getThreadingServices().getExecutorService().submit(r);
	}

		/**
		 * Called when checking the stored data on server start runs into an error. 
		 */
		protected void recoverInstanceActivationError(Exception e, String id, boolean logIt){
			Throwable cause=e;
			while(cause.getCause()!=null){
				cause=cause.getCause();
			}
			if(logIt){
				Log.logException("Problem reading stored data for <"+serviceName+">", e, logger);
			}
			try{
				if(ClassNotFoundException.class.isAssignableFrom(cause.getClass())){
					if(logIt)logger.info("Deleting stored data due to incompatible class change (server update?) for <{}>", serviceName);
					serviceInstances.remove(id);
				}
			}catch(Exception ex){}
		}

		/**
		 * When activating the Home, this method is called for each instance 
		 * It is used to populate some internal info, such as the number of instances
		 * per user
		 * 
		 * @param id
		 */
		protected void doPerInstanceActivation(String id) throws Exception {
			Pair<String, List<ACLEntry>> secInfo = readSecurityInfo(id);
			if(secInfo!=null)secInfoCache.put(id, secInfo);
		}

		private Pair<String, List<ACLEntry>> readSecurityInfo(String id) throws Exception {
			Resource r = serviceInstances.read(id);
			if(r!=null){
				if(r.getModel() instanceof SecuredResourceModel){
					SecuredResourceModel srm = (SecuredResourceModel)r.getModel();
					String owner = srm.getOwnerDN();	
					if(owner!=null && shouldCountForResourceLimit(r)){
						getInstancesPerUser(owner).incrementAndGet();
					}
					else{
						// set server as owner
						X509Credential kernelIdentity = getKernel().getContainerSecurityConfiguration().getCredential();
						if (kernelIdentity != null) {
							owner = kernelIdentity.getSubjectName();
							logger.debug("Setting server as owner of {}/{}", serviceName, id);
						}
						else {
							owner = Client.ANONYMOUS_CLIENT_DN;
						}
					}
					return new Pair<>(owner, srm.getAcl());
				}
			}
			return null;
		}




		protected boolean shouldCountForResourceLimit(Resource r) {
			return true;
		}

		/**
		 * setup the expiry check
		 * this implementation can be customised by
		 * setting two parameters:
		 * @see ContainerProperties#EXPIRYCHECK_INITIAL 
		 * @see ContainerProperties#EXPIRYCHECK_PERIOD
		 */
		protected void initExpiryCheck(Collection<String> uniqueIDs)throws Exception{
			instanceChecking.addAll(uniqueIDs);
			int initial = kernel.getContainerProperties().getSubkeyIntValue(
					ContainerProperties.EXPIRYCHECK_INITIAL, serviceName);
			int period = kernel.getContainerProperties().getSubkeyIntValue(
					ContainerProperties.EXPIRYCHECK_PERIOD, serviceName);
			logger.debug("[{}] Expiry thread scheduled at a period of {} secs.", serviceName, period);
			ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
			ts.getScheduledExecutorService().scheduleWithFixedDelay(instanceChecking,
					initial,period,TimeUnit.SECONDS);
		}

		public void runExpiryCheckNow(){
			try{
				instanceChecking.run();
			}catch(Exception e){
				logger.warn("["+serviceName+"] Uncaught exception occured while running expiry check",e);
			}
		}


		/**
		 * stops expiry checks 
		 */
		@Override
		public void stopExpiryCheckNow(){
			try{
				instanceChecking.removeChecker(expiryChecker);
			}catch(Exception e){
				logger.warn("["+serviceName+"] Uncaught exception occured while stopping expiry check",e);
			}
		}

		/**
		 * called when the container shuts down
		 */
		@Override
		public void shutdown(){
			if(isShuttingDown)return;
			isShuttingDown=true;
			logger.info("[{}] Shutting down.", serviceName);
			if (serviceInstances != null)
				serviceInstances.shutdown();
		}

		/**
		 * the container has been restarted or the container config has changed
		 */
		@Override
		public void notifyConfigurationRefresh(){
			lastRefreshNotificationInstant=System.currentTimeMillis();
		}

		@Override
		public String getServiceName(){return serviceName;}

		public void setServiceName(String serviceName){this.serviceName=serviceName;}

		@Override
		public Resource get(String id)throws ResourceUnknownException, ResourceUnavailableException{
			Resource res = null;
			try{
				res = serviceInstances.read(id);
			}catch(Exception e) {
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(id)+"> cannot be accessed",e);
			}
			if(res==null)throw new ResourceUnknownException("Instance with ID <"+buildFullServiceID(id)+"> does not exist");
			return res;
		}

		@Override
		public Resource refresh(String id) throws ResourceUnknownException, ResourceUnavailableException {
			try(Resource resource = serviceInstances.getForUpdate(id,locking_timeout,TimeUnit.SECONDS)){
				if(resource==null)throw new ResourceUnknownException("Instance with ID <"+id+"> does not exist");
				processMessages(resource);
				return resource;
			}catch(TimeoutException te){
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(id)+"> is not available.");
			}catch(Exception pe){
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(id)+"> cannot be accessed",pe);
			}
		}

		/**
		 * lock a resource which was already read from persistence. This is
		 * effectively upgrading a read to a write operation
		 * 
		 * @param resource
		 * @throws ResourceUnavailableException
		 */
		@Override
		public void lock(Resource resource) throws ResourceUnavailableException {
			try{
				serviceInstances.lock(resource,locking_timeout,TimeUnit.SECONDS);
				processMessages(resource);
			}catch(TimeoutException te){
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(resource.getUniqueID())+"> is not available.");
			}catch(Exception pe){
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(resource.getUniqueID())+"> cannot be accessed",pe);
			}
		}

		@Override
		public Resource getForUpdate(String id) throws ResourceUnknownException, ResourceUnavailableException {
			try{
				Resource resource = serviceInstances.getForUpdate(id,locking_timeout,TimeUnit.SECONDS);
				if(resource==null)throw new ResourceUnknownException("Instance with ID <"+id+"> does not exist");
				processMessages(resource);
				return resource;
			}catch(TimeoutException te){
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(id)+"> is not available.");
			}catch(Exception pe){
				throw new ResourceUnavailableException("Instance with ID <"+buildFullServiceID(id)+"> cannot be accessed",pe);
			}
		}

		private void processMessages(Resource r){
			PullPoint pp=null;
			try{
				if(kernel.getMessaging().hasMessages(r.getUniqueID())){
					pp=kernel.getMessaging().getPullPoint(r.getUniqueID());
					if(pp.hasNext()){
						r.processMessages(pp);
					}
				}
			}
			catch(Exception e){
			}finally{
				if(pp!=null)pp.dispose();
			}
		}

		private String buildFullServiceID(String resourceID){
			return kernel.getContainerProperties().getBaseUrl()+"/"+serviceName+"?res="+resourceID;
		}

		@Override
		public String createResource(InitParameters initParams) throws ResourceNotCreatedException {
			String owner = checkLimits();
			if(owner!=null) {
				getInstancesPerUser(owner).incrementAndGet();
			}
			try{
				Resource newInstance=doCreateInstance(initParams);
				newInstance.setHome(this);
				newInstance.setKernel(kernel);
				newInstance.initialise(initParams);
				postInitialise(newInstance);
				persist(newInstance);
				String uniqueID = newInstance.getUniqueID();
				instanceChecking.add(uniqueID);	
				return uniqueID;
			}
			catch(Exception e){
				String msg=Log.createFaultMessage("Resource not created.", e);
				throw new ResourceNotCreatedException(msg,e);
			}
		}

		/**
		 * invoked after the new resource has been initialised, and before it is stored
		 * @param instance - the newly created instance
		 */
		protected void postInitialise(Resource instance){}

		@Override
		public void persist(Resource instance)throws Exception{
			serviceInstances.persist(instance);
			if(instance.getModel() instanceof SecuredResourceModel){
				SecuredResourceModel srm = (SecuredResourceModel)instance.getModel();
				secInfoCache.put(instance.getUniqueID(), new Pair<>(srm.getOwnerDN(), srm.getAcl()));
			}
		}

		@Override
		public Calendar getTerminationTime(String uniqueID)throws Exception{
			updateTT();
			return terminationTimes.get(uniqueID);
		}

		//re-load tt time map from the persistence layer
		protected void updateTT()throws Exception{
			synchronized (ttLock) {
				if(System.currentTimeMillis()-lastAccessed<tt_update_interval)return;
				if(serviceInstances!=null){
					terminationTimes = serviceInstances.getTerminationTimes();
				}
				lastAccessed=System.currentTimeMillis();
			}
		}

		@Override
		public void setTerminationTime(String uniqueID, Calendar c)throws TerminationTimeChangeRejectedException,UnableToSetTerminationTimeException{
			//check if maximum termination time is exceeded
			Integer maxLifetime=null;
			if(kernel!=null){ // TODO can be null in unit tests -> should refactor
				maxLifetime=getKernel().getContainerProperties().getSubkeyIntValue(ContainerProperties.MAXIMUM_LIFETIME, serviceName);
			}
			if(maxLifetime!=null){
				boolean exceeded=false;
				if(c==null){
					//infinite LT was requested
					exceeded=true;
				}
				else{
					long req=(c.getTimeInMillis()-System.currentTimeMillis())/1000;
					if(req>maxLifetime){
						exceeded=true;
					}
				}
				if(exceeded){
					throw new TerminationTimeChangeRejectedException("Requested lifetime is larger than maximum configured on the system.");
				}
			}
			try{
				if(serviceInstances!=null){
					serviceInstances.setTerminationTime(uniqueID, c);
				}
				if(c!=null){
					terminationTimes.put(uniqueID,c);
				}
				else{
					terminationTimes.remove(uniqueID);
				}
			}catch(Exception e){
				throw new UnableToSetTerminationTimeException(e);
			}
		}

		@Override
		public String getOwner(String resourceID){
			Pair<String,List<ACLEntry>>secInfo = secInfoCache.get(resourceID);
			return secInfo!=null ? secInfo.getM1() : null;
		}

		/**
		 * You must override this in subclasses to actually create the instance.
		 * In case you need to access the init parameters, override the
		 * {@link #doCreateInstance(InitParameters initParams)} method
		 */
		protected abstract Resource doCreateInstance()throws Exception;

		/**
		 * You may override in subclasses to create the instance. 
		 * The default implementation simply delegates to {@link #doCreateInstance()}
		 */
		protected Resource doCreateInstance(InitParameters initParams)throws Exception{
			return doCreateInstance();
		}

		/**
		 * remove resource from persistent storage
		 * 
		 * @param resourceId - the ID of the resource to remove
		 */
		public void removeFromStorage(String resourceId) throws Exception{
			serviceInstances.remove(resourceId);
			terminationTimes.remove(resourceId);
		}

		/**
		 * remove resource from all internal data structures
		 * 
		 * @param resourceId - the ID of the resource to remove
		 */
		@Override
		public void destroyResource(String resourceId) throws Exception{
			removeFromStorage(resourceId);
			instanceChecking.remove(resourceId);
			secInfoCache.remove(resourceId);
		}

		@Override
		public long getNumberOfInstances()throws Exception{
			return serviceInstances.size();
		}

		@Override
		public Store getStore() {
			return serviceInstances;
		}

		public void setStore(Store serviceInstances) {
			this.serviceInstances = serviceInstances;
		}

		@Override
		public boolean isShuttingDown(){
			return isShuttingDown;
		}

		public boolean supportsNotification(){
			return supportsNotification;
		}

		/**
		 * Initialise notification support. This default implementation does nothing.
		 */
		protected void initNotification()throws MessagingException{
		}

		/**
		 * check whether the current user's limit of service instances has not yet been exceeded
		 * @throws ResourceNotCreatedException - if limit exceeded
		 */
		protected String checkLimits() throws ResourceNotCreatedException{
			String owner=null;
			try{
				SecurityTokens tokens=AuthZAttributeStore.getTokens();
				if(tokens!=null && tokens.getEffectiveUserName()!=null){
					owner = tokens.getEffectiveUserName();
				}
				else{
					logger.debug("No security information available.");
				}
			}catch(Exception ex){
				Log.logException("Error processing security information.", ex, logger);
			}
			if(owner!=null){
				AtomicInteger num=getInstancesPerUser(owner);
				int current = num.get();
				if(current>=getInstanceLimit(owner)){
					ResourceNotCreatedException rnc=new ResourceNotCreatedException("Limit of <"
							+current+"> instances of <"+serviceName+"> for <"+owner+"> has been reached.");
					rnc.setErrorCode(ResourceNotCreatedException.ERR_INSTANCE_LIMIT_EXCEEDED);
					throw rnc;
				}
			}
			return owner;
		}

		protected int getInstanceLimit(String owner){
			return kernel.getContainerProperties().getSubkeyIntValue(
					ContainerProperties.MAX_INSTANCES, serviceName);
		}

		public void instanceDestroyed(String owner){
			if(owner!=null){
				AtomicInteger num=getInstancesPerUser(owner);
				if(num.intValue()>0)num.decrementAndGet();
			}
		}

		private AtomicInteger getInstancesPerUser(String owner){
			if(owner == null)return null;
			AtomicInteger num=null;
			synchronized (instancesPerUser) {
				num=instancesPerUser.get(owner);
				if(num==null){ //can happen after a restart
					num=new AtomicInteger(0);
					instancesPerUser.put(owner, num);
				}
			}
			return num;
		}

		/**
		 * get the last time the configuration was refreshed or the server restarted
		 */
		public long getLastRefreshInstant(){
			return lastRefreshNotificationInstant;
		}

		/**
		 * Post-server startup task. It is executed after server start, but before any user-defined startup tasks. 
		 * 
		 * By default, nothing is done here.
		 */
		@Override
		public void run(){
			//NOP
		}

		@Override
		public Kernel getKernel(){
			return kernel;
		}

		@Override
		public void setKernel(Kernel kernel){
			this.kernel=kernel;
		}

		/**
		 * return a read-only copy of the map holding the number of resources per user DN
		 */
		public Map<String, AtomicInteger>getInstancesPerUser(){
			return Collections.unmodifiableMap(instancesPerUser);
		}

		@Override
		public List<String>getAccessibleResources(Client client) throws Exception {
			return getAccessibleResources(getStore().getUniqueIDs(), client);
		}

		@Override
		public List<String>getAccessibleResources(Collection<String> ids, Client client) {
			SecurityManager sec = kernel.getSecurityManager();
			List<String>accessible=new ArrayList<>();
			for(String id: ids){
				Pair<String, List<ACLEntry>> secInfo = secInfoCache.get(id);
				String ownerDN = secInfo.getM1();
				List<ACLEntry> acl = secInfo.getM2();
				try{
					if(sec.isAccessible(client, serviceName, id, ownerDN, acl)){
						accessible.add(id);
					}
				}catch(Exception ex){
					Log.logException("["+serviceName+"] Error checking accessibility", ex, Log.getLogger(Log.SERVICES, DefaultHome.class));
				}
			}
			return accessible;
		}

		public List<String>getTaggedResources(String...tags) throws Exception {
			return getStore().getTaggedResources(tags);
		}
	}
