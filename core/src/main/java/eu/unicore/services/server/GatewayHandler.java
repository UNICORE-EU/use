package eu.unicore.services.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.DefaultContainerSecurityConfiguration;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ConnectionUtil;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Encapsulates the interaction with a Gateway.
 * 
 * TODO: this should be able to communicate with Gw in non-SSL mode too, but this requires
 * development on Gw side as well.
 * @author schuller
 */
public class GatewayHandler implements ExternalSystemConnector{

	private static final Logger logger=Log.getLogger(Log.UNICORE, GatewayHandler.class);

	private final ContainerProperties containerConfiguration;
	private final IClientConfiguration clientConfiguration;
	private final DefaultContainerSecurityConfiguration secConfiguration;
	private final ThreadingServices threadingSrv;

	private Status status=Status.UNKNOWN;
	private String statusMessage;
	private long lastChecked;

	public GatewayHandler(ContainerProperties containerConfiguration, 
			IClientConfiguration clientConfiguration,
			DefaultContainerSecurityConfiguration secConfiguration){
		this.containerConfiguration = containerConfiguration;
		this.clientConfiguration = clientConfiguration;
		this.secConfiguration = secConfiguration;
		this.threadingSrv = containerConfiguration.getThreadingServices();
	}

	/**
	 * wait until a connection to the Gateway has been established
	 */
	public void waitForGateway() throws Exception {
		if (!secConfiguration.isGatewayWaitingEnabled())
			return;
		
		Integer timeout = secConfiguration.getGatewayWaitTime();
		timeout *= 1000;
		long start = System.currentTimeMillis();
		String gwUrl = containerConfiguration.getValue(ContainerProperties.EXTERNAL_URL);
		do{
			try {
				X509Certificate[] cert = ConnectionUtil.getPeerCertificate(clientConfiguration, gwUrl, 
						(int)(timeout-(System.currentTimeMillis()-start)), logger);
				logger.info("Successfully connected to gateway at {}", gwUrl);
				if (secConfiguration.isGatewaySignatureCheckingEnabled() && 
						secConfiguration.getGatewayCertificate() == null) {
					secConfiguration.setGatewayCertificate(cert[0]);
					logger.info("Gateway's DN was autodetected and will be used for signature checking: {}",
							X500NameUtils.getReadableForm(secConfiguration.
									getGatewayCertificate().getSubjectX500Principal()));
				}
				return;
			} catch (SSLException ssl){
				// do not be silent about this, since it usually means there
				// is some config issue that the admin should know about
				throw ssl;
			} catch (Exception e1) { /* ok, ignored */	}
			logger.info("Waiting for gateway...");
			if (timeout < System.currentTimeMillis()-start)
				break;
			try{
				Thread.sleep(2000);
			}catch(InterruptedException e){}
		} while(true);
		throw new Exception("The Gateway is not available and the server is configured " +
				"to wait for it (the property '" + 
				ContainerSecurityProperties.PREFIX+ContainerSecurityProperties.PROP_GATEWAY_WAIT + "')");
	}

	public String getExternalSystemName(){
		return "Gateway";
	}

	public String getConnectionStatusMessage(){
		checkConnection();	
		return statusMessage;
	}

	public Status getConnectionStatus(){
		checkConnection();
		return status;
	}

	private void checkConnection(){
		if (lastChecked+2000>System.currentTimeMillis())
			return;
		String url = containerConfiguration.getValue(ContainerProperties.EXTERNAL_URL);
		String myHost = containerConfiguration.getValue(ContainerProperties.SERVER_HOST) + ":"
				+containerConfiguration.getValue(ContainerProperties.SERVER_PORT);
		boolean secure = clientConfiguration.isSslEnabled();
		if(url.contains(myHost)){
			status=Status.NOT_APPLICABLE;
			statusMessage="N/A (no gateway used)";
		}
		else{
			Boolean result = TimeoutRunner.compute(getCheckConnectionTask(url, secure), threadingSrv, 2000);
			if(result!=null && result){
				status=Status.OK;
				statusMessage="OK [connected to "+url+"]";
			}
			else {
				status=Status.DOWN;
				statusMessage="CAN'T CONNECT TO GATEWAY "+url;
			}
		}
		lastChecked=System.currentTimeMillis();
	}

	/**
	 * setup dynamic gateway registration, configured using the following properties
	 * <ul>
	 *  <li>{@link ContainerSecurityProperties#PROP_AUTOREGISTER_WITH_GATEWAY}</li>
	 *  <li>{@link ContainerSecurityProperties#PROP_AUTOREGISTER_WITH_GATEWAY_UPDATE}</li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	public void enableGatewayRegistration()throws Exception{
		if(!secConfiguration.isGatewayRegistrationEnabled()){
			return;
		}
		Integer update=secConfiguration.getGatewayRegistrationUpdateInterval();
		logger.info("Enabling dynamic registration at the Gateway at {} updated every {} seconds",
				Utilities.getGatewayAddress(containerConfiguration), update);
		new GatewayRegistration(containerConfiguration, update);
		return;
	}


	private Callable<Boolean> getCheckConnectionTask(final String url, final boolean secure) {
		Callable<Boolean> getCert=new Callable<Boolean>(){
			public Boolean call() {
				try {
					if (secure)
						ConnectionUtil.getPeerCertificate(clientConfiguration,  
								url, 2000, logger);
					else {
						throw new RuntimeException("Insecure Gw " +
								"communication is not yet implemented");
					}
					return true;
				} catch (UnknownHostException e) {
					logger.warn("Gateway host is unknown: " + e);
					return false;
				} catch (IOException e) {
					logger.warn("Can't contact gateway: " + e);
					return false;
				}
			}
		};
		return getCert;
	}

	/**
	 * registers with a gateway and updates the registration periodically
	 * 
	 * @author schuller
	 */
	public class GatewayRegistration implements Runnable {

		private HttpClient client;
		private final String gwAddress;
		
		private final ContainerProperties containerConfiguration;
		
		/**
		 * creates a new GatewayRegistration using the default update
		 * interval of 60 seconds
		 */
		public GatewayRegistration(ContainerProperties containerConfiguration) throws Exception {
			this(containerConfiguration, 60);
		}

		/**
		 * creates a new GatewayRegistration
		 * @param updateInterval - in seconds
		 */
		public GatewayRegistration(ContainerProperties containerConfiguration, int updateInterval)
				throws Exception{
			this.containerConfiguration=containerConfiguration;
			gwAddress = extractGWAddress(containerConfiguration.getValue(ContainerProperties.EXTERNAL_URL))
					+"/VSITE_REGISTRATION_REQUEST";
			client = HttpUtils.createClient(gwAddress, clientConfiguration);
			threadingSrv.getScheduledExecutorService().scheduleWithFixedDelay(
					this, 0, 1000*updateInterval, TimeUnit.MILLISECONDS);	
		}

		public void run(){
			HttpPost post = new HttpPost(gwAddress);
			try{
		        List<NameValuePair> nvps = new ArrayList<>();
		        String vsiteName=containerConfiguration.getValue(ContainerProperties.VSITE_NAME_PROPERTY);
		        nvps.add(new BasicNameValuePair("name", vsiteName));
				String physAddr = Utilities.getPhysicalServerAddress(containerConfiguration, 
						clientConfiguration.isSslEnabled());
		        nvps.add(new BasicNameValuePair("address", physAddr));
		        String secret = secConfiguration.getGatewayRegistrationSecret();
		        nvps.add(new BasicNameValuePair("secret", secret));
		        post.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
		        try(ClassicHttpResponse response = client.executeOpen(null, post, HttpClientContext.create())){
		        	if(response.getCode() != HttpStatus.SC_CREATED){
		        		logger.warn("Could not register with gateway at {}, will try again!",gwAddress);
		        	}
		        	EntityUtils.consumeQuietly(response.getEntity());
		        }
				logger.debug("(Re-)registered with gateway at {}", gwAddress);
			}catch(Exception e){
				Log.logException("Could not contact gateway at "+gwAddress,e,logger);
			}
		}

		private String extractGWAddress(String base)throws MalformedURLException{
			URL u=new URL(base);
			return u.toString().split(u.getPath())[0];
		}
	}
}
