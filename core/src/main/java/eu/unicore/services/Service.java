package eu.unicore.services;

/**
 * a generic service hosted in USE
 */
public interface Service {

	/**
	 * get the service name
	 */
	public String getName();
	
	/**
	 * get the service type
	 */
	public String getType();
	
	/**
	 * start the service. The service needs to do any service-type specific initialisation, registration, etc
	 * at this stage.
	 */
	public void start() throws Exception;
	
	/**
	 * stop the service (possibly temporarily). The service needs to de-register from any type-specific 
	 * registries
	 */
	public void stop() throws Exception;
	
	/**
	 * stop and perform cleanup (e.g. remove database content)
	 * This is called prior to undeployment
	 */
	public void stopAndCleanup() throws Exception;

	/**
	 * check whether the service is started
	 */
	public boolean isStarted();

	/**
	 * get the {@link Home} for this service, or <code>null</code> if 
	 * not applicable (e.g. for a "plain" web service)
	 */
	public Home getHome();
	
	/**
	 * get the class name that defines the service interface, or <code>null</code> if
	 * not applicable
	 */
	public String getInterfaceClass();
}
