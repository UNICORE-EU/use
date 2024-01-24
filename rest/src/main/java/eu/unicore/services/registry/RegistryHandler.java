package eu.unicore.services.registry;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ExternalSystemConnector.Status;
import eu.unicore.services.Home;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.RESTException;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.services.rest.client.Resources;
import eu.unicore.services.rest.registry.Registries;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.util.Log;

/**
 * It is used to obtain a client for both internal and external <b>default</b> registries, which are
 * configured container-wide. 
 *
 * @author demuth
 * @author schuller
 */
public class RegistryHandler implements ISubSystem {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".registry", RegistryHandler.class);

	/**
	 * the set of external registry URLs. It is updated whenever the property file
	 * changes using the {@link #updateExternalRegistryURLs()} method
	 */
	private final Set<String> externalRegistryURLs = new HashSet<>();
	private LocalRegistryClient registryClient = null;
	protected ScheduledExecutorService scheduledExecutor = null;
	private final Kernel kernel;
	private boolean isGlobalRegistry = false; 
	private ContainerProperties config;
	private String statusMessage = "";
	private Status status = Status.UNKNOWN;

	private final Collection<ExternalSystemConnector> connectors = new ArrayList<>();

	public RegistryHandler(Kernel kernel)throws Exception{
		this.kernel=kernel;
		Home regHome=kernel.getHome(RegistryCreator.SERVICE_NAME);
		if (regHome !=null && regHome instanceof RegistryHomeImpl){
			isGlobalRegistry = true;
		}
		reloadConfig(kernel);
		if(regHome==null)
			return;
		registryClient = new LocalRegistryClient(kernel);
		updateStatusMessage();
	}
	
	@Override
	public void reloadConfig(Kernel kernel) {
		this.config = kernel.getContainerProperties();
		updateExternalRegistryURLs();
	}

	public static synchronized RegistryHandler get(Kernel kernel) throws Exception {
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		if(rh==null) {
			rh = new RegistryHandler(kernel);
			kernel.setAttribute(RegistryHandler.class, rh);
			kernel.register(rh);
		}
		return rh;
	}

	/**
	 * this is only used internally
	 * Application code should retrieve a {@link RegistryClient} using {@link #getExternalRegistryClient()}
	 * @returns the current list of external registry URLs
	 */
	public synchronized String[] getExternalRegistryURLs(){
		return externalRegistryURLs.toArray(new String[externalRegistryURLs.size()]);
	}

	/**
	 * updates the set of external registry URLs (without clearing it first)
	 */
	private void updateExternalRegistryURLs(){
		if (isGlobalRegistry) logger.debug("This is a global registry. No external registry used.");
		boolean usesExternal = usesExternalRegistry();
		if (!usesExternal) logger.debug("Usage of external registry is switched off. " +
				"No external registry used.");

		if(!isGlobalRegistry && usesExternal) {
			logger.debug("Determining external registry address(es) ...");
			synchronized (externalRegistryURLs) {
				connectors.clear();
				List<String> registryUrls = config.getListOfValues(
						ContainerProperties.EXTERNAL_REGISTRY_KEY);
				for(String registryURL: registryUrls){
					if(registryURL!=null && registryURL.length()>0){
						String u = registryURL.trim();
						if(!u.contains("/rest/registries/")) {
							u = Registries.convertToREST(u);
						}
						externalRegistryURLs.add(u);
						RConnector rc = new RConnector(u);
						connectors.add(rc);
						logger.info("Using registry: {}", u);
					}
				}
			}
		}
		updateStatusMessage();
	}

	/**
	 * add an the external registry URL
	 */
	protected void addExternalRegistryURL(String url){
		synchronized (externalRegistryURLs) {
			externalRegistryURLs.add(url);
		}
	}

	/**
	 * get a client for the internal registry 
	 */
	public LocalRegistryClient getRegistryClient()throws Exception{
		return registryClient;
	}
	
	public boolean usesExternalRegistry() {
		return config.getBooleanValue(ContainerProperties.EXTERNAL_REGISTRY_USE);
	}

	public boolean isSharedRegistry() {
		return "shared".equals(config.getValue("feature.Registry.mode"));
	}
	
	/**
	 * get a client for talking to the external registries
	 * @return {@link ExternalRegistryClient}
	 */ 
	public ExternalRegistryClient getExternalRegistryClient()throws Exception{
		if(!usesExternalRegistry())return null;
		synchronized(externalRegistryURLs){
			return ExternalRegistryClient.getExternalRegistryClient(
					externalRegistryURLs, kernel.getClientConfiguration());
		}
	}

	@Override
	public String getName() {
		return "Registry";
	}
	
	@Override
	public String getStatusDescription(){
		return statusMessage;
	}

	@Override
	public Collection<ExternalSystemConnector>getExternalConnections(){
		return connectors;
	}

	private void updateStatusMessage() {
		if(isGlobalRegistry) {
			statusMessage = "Shared";
		}
		else {
			if (externalRegistryURLs.size()==0){
				statusMessage = "N/A";
			}
			else {
				statusMessage="Ext URLs: " + externalRegistryURLs.toString();
			}
		}
	}
	
	public void updatePublicKeys(){
		Status oldStatus = status;
		try{
			ExternalRegistryClient erc = getExternalRegistryClient();
			if(erc==null)return;
			PubkeyCache keyCache = PubkeyCache.get(kernel);
			for(Map<String,String>entry: erc.listEntries()){
				try{
					String pem = entry.get(RegistryClient.SERVER_PUBKEY);
					if(pem!=null){
						String serverDN = entry.get(RegistryClient.SERVER_IDENTITY);
						keyCache.update(serverDN, parsePEM(pem));
						logger.debug("Read public key for <{}>", serverDN);
					}
				}catch(Exception ex){}
			}
			status = Status.OK;
		}catch(Exception ex){
			status = Status.DOWN;
			if(status!=oldStatus) {
				Log.logException("Error updating public keys from external registry", ex, logger);
			}
		}
	}
	
	private PublicKey parsePEM(String pem) throws Exception {
		X509Certificate cert = CertificateUtils.loadCertificate(
				IOUtils.toInputStream(pem, "UTF-8"), Encoding.PEM);
		return cert.getPublicKey();
	}
	
	private class RConnector implements ExternalSystemConnector {
		private Status status = Status.NOT_APPLICABLE;
		private String statusMessage = "N/A";
		private long lastChecked;
		private final String url;
		
		public RConnector(String url) {
			this.url = url;
		}
		
		@Override
		public String getConnectionStatusMessage() {
			checkConnection();
			return statusMessage;
		}

		@Override
		public Status getConnectionStatus() {
			checkConnection();
			return status;
		}

		@Override
		public String getExternalSystemName() {
			return "Registry ["+url+"]";
		}

		private void checkConnection() {
			switch(status) {
			case NOT_APPLICABLE:
			case OK:
				if (lastChecked+60000<System.currentTimeMillis()) {
					lastChecked = System.currentTimeMillis();
					final RegistryClient rc = new RegistryClient(url, kernel.getClientConfiguration());
					Callable<String>task = () -> {
						try {
							rc.getJSON();
							return "OK";
						}catch(RESTException re) {
							return re.getErrorMessage();
						}catch(Exception e) {
							return Log.createFaultMessage("Error ", e);
						}
					};
					String res = compute(task, 5000);
					if(!"OK".equals(res)){
						statusMessage=res;
						status = Status.DOWN;
					}
					else {
						status = Status.OK;
					}
				}
				break;
			default:
				break;
			}
		}

		private String compute(Callable<String>task, int timeout){
			try{
				Future<String> f = Resources.getExecutorService().submit(task);
				return f.get(timeout, TimeUnit.MILLISECONDS);
			}catch(Exception ex){
				return "ERROR";
			}
		}
	}
}
