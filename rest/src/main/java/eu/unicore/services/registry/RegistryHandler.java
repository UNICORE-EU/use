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

package eu.unicore.services.registry;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.services.rest.registry.Registries;
import eu.unicore.services.security.util.PubkeyCache;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.PropertyChangeListener;

/**
 * It is used to obtain a client for both internal and external <b>default</b> registries, which are
 * configured container-wide. 
 *
 * @author demuth
 * @author schuller
 */
public class RegistryHandler implements ExternalSystemConnector {

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

	private Status status=Status.NOT_APPLICABLE;
	private String statusMessage;
	private long lastChecked;

	public RegistryHandler(Kernel kernel)throws Exception{
		this.kernel=kernel;
		this.config = kernel.getContainerProperties();
		
		config.addPropertyChangeListener(new PropertyChangeListener() {
			private final String[] PROPS = new String[] {ContainerProperties.EXTERNAL_REGISTRY_USE,
					ContainerProperties.EXTERNAL_REGISTRY_KEY};
			
			@Override
			public void propertyChanged(String propertyKey)	{
				logger.info("Registry settings update detected: updating external registry settings.");
				synchronized(this){
					externalRegistryURLs.clear();
					updateExternalRegistryURLs();
				}
			}
			
			@Override
			public String[] getInterestingProperties() {
				return PROPS;
			}
		});
		
		Home regHome=kernel.getHome(RegistryCreator.SERVICE_NAME);
		if (regHome !=null && regHome instanceof RegistryHomeImpl){
			isGlobalRegistry = true;
		}
		
		updateExternalRegistryURLs();

		if(regHome==null)
			return;
		
		registryClient = new LocalRegistryClient(kernel);	
		
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
				if(externalRegistryURLs.size() == 0) {
					List<String> registryUrls = config.getListOfValues(
							ContainerProperties.EXTERNAL_REGISTRY_KEY);
					for(String registryURL: registryUrls){
						if(registryURL!=null && registryURL.length()>0){
							String u = registryURL.trim();
							if(!u.contains("/rest/registries/")) {
								u = Registries.convertToREST(u);
							}
							externalRegistryURLs.add(u);
							logger.info("Using registry: {}", u);
						}
					}
				}
			}
			if (externalRegistryURLs.size()==0){
				logger.warn("No external registry URLs are defined!");
			}
		}
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
	public String getExternalSystemName() {
		return "Registry";
	}
	
	@Override
	public String getConnectionStatusMessage(){
		checkConnection();	
		return statusMessage;
	}
	
	@Override
	public Status getConnectionStatus(){
		checkConnection();
		return status;
	}

	private void checkConnection(){
		if (lastChecked+2000>System.currentTimeMillis())
			return;
		
		try{
			if(isSharedRegistry()){
				status = Status.NOT_APPLICABLE;
				statusMessage = "N/A (this is a shared Registry)";
			}
			else if(!usesExternalRegistry()){
				status = Status.NOT_APPLICABLE;
				statusMessage = "N/A (no external Registry used)";
			}
			else {
				String[]urls=getExternalRegistryURLs();
				ExternalRegistryClient erc=getExternalRegistryClient();
				if(erc.checkConnection()){
					String allurls="";
					for(String u: urls){
						allurls+=u+" ";
					}
					status = Status.OK;
					statusMessage = "OK [connected to "+allurls+"]";
				}
				else{
					status=Status.DOWN;
					statusMessage = "CAN'T CONNECT ["+erc.getConnectionStatus()+"]";
				}
			}
		}catch(Exception ex){
			status=Status.DOWN;
			statusMessage = Log.createFaultMessage("Error! ",ex);
		}
		
		lastChecked=System.currentTimeMillis();
	}
	
	public void updatePublicKeys(){
		try{
			checkConnection();
			if(Status.OK.equals(status)){
				ExternalRegistryClient erc = getExternalRegistryClient();
				PubkeyCache keyCache = PubkeyCache.get(kernel);
				if(erc==null)return;
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
			}
		}catch(Exception ex){
			Log.logException("Error updating public keys from external registry", ex, logger);
		}
	}
	
	private PublicKey parsePEM(String pem) throws Exception {
		X509Certificate cert = CertificateUtils.loadCertificate(
				IOUtils.toInputStream(pem, "UTF-8"), Encoding.PEM);
		return cert.getPublicKey();
	}
	
}
