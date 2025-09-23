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
import eu.unicore.services.exceptions.InvalidModificationException;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.persistence.Store;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.SecurityManager;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.utils.LoadingMap;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

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

	protected Kernel kernel;

	protected final Map<String, AtomicInteger> instancesPerUser = new ConcurrentHashMap<>();

	private volatile boolean isShuttingDown=false;

	/**
	 * this takes care of removing expired Resources etc.
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
				new Function<>() {
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
		this.serviceName = serviceName;
		serviceInstances = kernel.getPersistenceManager().getPersist(serviceName);
		Collection<String> uniqueIDs = serviceInstances.getUniqueIDs();
		logger.info("[{}] Have {} instances from permanent storage.", serviceName, uniqueIDs.size());
		asyncInit(uniqueIDs);
		locking_timeout = kernel.getContainerProperties().
				getSubkeyIntValue(ContainerProperties.INSTANCE_LOCKING_TIMEOUT, serviceName);
	}

	protected void asyncInit(Collection<String> uniqueIDs) throws Exception {
		kernel.getContainerProperties().getThreadingServices().getExecutorService().submit(
		()->{
			Iterator<String>iter = uniqueIDs.iterator();
			int errors = 0;
			while(iter.hasNext()){
				String id = iter.next();
				try{
					doPerInstanceActivation(id);
				}
				catch(Exception e){
					recoverInstanceActivationError(e, id);
					errors++;
					iter.remove();
				}
			}
			if(errors>0)logger.warn("There were <{}> errors during service activation!", errors);
			initExpiryCheck(uniqueIDs);
			logger.info("[{}] Initialisation done.", serviceName);
		});
	}

	/**
	 * Called when checking the stored data on server start runs into an error.
	 */
	protected void recoverInstanceActivationError(Exception e, String id){
		Throwable cause=e;
		while(cause.getCause()!=null){
			cause=cause.getCause();
		}
		if(ClassNotFoundException.class.isAssignableFrom(cause.getClass())){
			try{
				serviceInstances.remove(id);
				logger.info("Deleted incompatible stored data (server update?) <{}/{}>", serviceName, id);
			}catch(Exception ex){}
		}
	}

	/**
	 * When activating the Home, this method is called for each instance
	 * It is used to populate some internal info, such as the number of instances
	 * per user
	 *
	 * @param id
	 */
	protected void doPerInstanceActivation(String id) throws Exception {
		var secInfo = readSecurityInfo(id);
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
					X509Credential kernelIdentity = kernel.getContainerSecurityConfiguration().getCredential();
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
	protected void initExpiryCheck(Collection<String> uniqueIDs) {
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

	@Override
	public void runExpiryCheckNow(){
		try{
			instanceChecking.run();
		}catch(Exception e){
			logger.warn("[{}] Uncaught exception occured while running expiry check", serviceName, e);
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

	@Override
	public Resource get(String id)throws ResourceUnknownException, ResourceUnavailableException{
		Resource res = null;
		try{
			res = serviceInstances.read(id);
		}catch(Exception e) {
			throw new ResourceUnavailableException("Instance with ID <"+_fullID(id)+"> cannot be accessed",e);
		}
		if(res==null)throw new ResourceUnknownException("Instance with ID <"+_fullID(id)+"> does not exist");
		res.setHome(this);
		return res;
	}

	@Override
	public Resource refresh(String id) throws Exception {
		try(Resource resource = getForUpdate(id)){
			return resource;
		}
	}

	@Override
	public Resource getForUpdate(String id) throws ResourceUnknownException, ResourceUnavailableException {
		try{
			Resource resource = serviceInstances.getForUpdate(id,locking_timeout,TimeUnit.SECONDS);
			if(resource==null)throw new ResourceUnknownException("Instance with ID <"+id+"> does not exist");
			resource.setHome(this);
			processMessages(resource);
			return resource;
		}catch(TimeoutException te){
			throw new ResourceUnavailableException("Instance with ID <"+_fullID(id)+"> is not available.");
		}catch(Exception pe){
			throw new ResourceUnavailableException("Instance with ID <"+_fullID(id)+"> cannot be accessed",pe);
		}
	}

	private void processMessages(Resource r){
		PullPoint pp = null;
		try{
			if(kernel.getMessaging().hasMessages(r.getUniqueID())){
				pp = kernel.getMessaging().getPullPoint(r.getUniqueID());
				if(pp.hasNext()){
					r.processMessages(pp);
				}
			}
		}
		catch(Exception e){}
		finally{
			if(pp!=null)pp.dispose();
		}
	}

	private String _fullID(String resourceID){
		return serviceName+":"+resourceID;
	}

	@Override
	public String createResource(InitParameters initParams) throws ResourceNotCreatedException {
		String owner = checkLimits();
		if(owner!=null) {
			getInstancesPerUser(owner).incrementAndGet();
		}
		try(Resource newInstance = doCreateInstance(initParams)){
			newInstance.setHome(this);
			newInstance.setKernel(kernel);
			newInstance.initialise(initParams);
			postInitialise(newInstance);
			String uniqueID = newInstance.getUniqueID();
			instanceChecking.add(uniqueID);
			return uniqueID;
		}
		catch(Exception e){
			throw new ResourceNotCreatedException(Log.createFaultMessage("Resource not created.", e), e);
		}
	}

	/**
	 * invoked after the new resource has been initialised, and before it is stored
	 * @param instance - the newly created instance
	 */
	protected void postInitialise(Resource instance){}

	@Override
	public void done(Resource instance)throws Exception{
		String owner = null;
		if(instance.getModel() instanceof SecuredResourceModel){
			SecuredResourceModel srm = (SecuredResourceModel)instance.getModel();
			owner = srm.getOwnerDN();
		}	
		if(instance.isDestroyed()) {
			cleanupResource(instance.getUniqueID(), owner);
		}
		else {
			serviceInstances.persist(instance);
			if(instance.getModel() instanceof SecuredResourceModel){
				SecuredResourceModel srm = (SecuredResourceModel)instance.getModel();
				secInfoCache.put(instance.getUniqueID(), new Pair<>(srm.getOwnerDN(), srm.getAcl()));
			}
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

	protected Integer getMaxLifetime() {
		return kernel.getContainerProperties().getSubkeyIntValue(ContainerProperties.MAXIMUM_LIFETIME,
				serviceName);
	}

	@Override
	public void setTerminationTime(String uniqueID, Calendar c) throws Exception {
		//check if maximum termination time is exceeded
		Integer maxLifetime = getMaxLifetime();
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
				throw new InvalidModificationException("Requested lifetime is larger than maximum configured on the system.");
			}
		}
		if(serviceInstances!=null){
			serviceInstances.setTerminationTime(uniqueID, c);
		}
		if(c!=null){
			terminationTimes.put(uniqueID,c);
		}
		else{
			terminationTimes.remove(uniqueID);
		}
	}

	@Override
	public String getOwner(String resourceID){
		Pair<String,List<ACLEntry>>secInfo = secInfoCache.get(resourceID);
		return secInfo!=null ? secInfo.getM1() : null;
	}

	/**
	 * Perform resource creation. In case access to the init parameters is needed,
	 * override the {@link #doCreateInstance(InitParameters initParams)} method
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
	 * remove resource from all data structures
	 * 
	 * @param resourceId - the ID of the resource to remove
	 * @param owner - the owner DN of the resource
	 */
	protected void cleanupResource(String resourceId, String owner) throws Exception{
		serviceInstances.remove(resourceId);
		terminationTimes.remove(resourceId);
		instanceChecking.remove(resourceId);
		secInfoCache.remove(resourceId);
		if(owner!=null){
			AtomicInteger num = getInstancesPerUser(owner);
			if(num.intValue()>0)num.decrementAndGet();
		}
	}

	@Override
	public Store getStore() {
		return serviceInstances;
	}

	@Override
	public boolean isShuttingDown(){
		return isShuttingDown;
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
				throw new ResourceNotCreatedException("Limit of <"
						+current+"> instances of <"+serviceName+"> for <"+owner+"> has been reached.");
			}
		}
		return owner;
	}

	protected int getInstanceLimit(String owner){
		return kernel.getContainerProperties().getSubkeyIntValue(
				ContainerProperties.MAX_INSTANCES, serviceName);
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
	 * Post-server startup task. It is executed after server start,
	 * but before any user-defined startup tasks.
	 *
	 * By default, nothing is done here.
	 */
	@Override
	public void run(){
		//NOP
	}

//	@Override
//	public Kernel getKernel(){
//		return kernel;
//	}

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

}