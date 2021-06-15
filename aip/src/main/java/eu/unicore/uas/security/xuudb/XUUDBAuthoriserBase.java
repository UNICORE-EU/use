package eu.unicore.uas.security.xuudb;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ConnectionUtil;

/**
 * get authorisation attributes for users by asking an XUUDB
 * 
 * @author schuller
 * @author golbi
 * @author piernikp
 */
public abstract class XUUDBAuthoriserBase implements IAttributeSourceBase, ExternalSystemConnector {

	private static final Logger logger = Log.getLogger(Log.SECURITY,
			XUUDBAuthoriserBase.class);
	public static final int DEFAULT_PORT = 34463;
	public static final String DEFAULT_HOST = "https://localhost";

	protected String name;
	protected boolean isEnabled = false;
	protected Integer port;
	protected String host;
	protected Boolean cacheCredentials = Boolean.TRUE;
	protected Kernel kernel;

	protected CircuitBreaker cb;

	public Integer getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}

	public String getGcID() {
		return gcID;
	}

	protected String xuudbURL = null;
	protected String gcID;
	protected CredentialCache cache;

	protected Status status = Status.UNKNOWN;
	protected String statusMessage = "N/A";
	
	public void configure(String name) {
		this.name = name;
		cb = new CircuitBreaker(name);
		if (port == null)
			port = DEFAULT_PORT;
		if (host == null)
			host = DEFAULT_HOST;

		xuudbURL = host + ":" + port + "/";
		logger.info("Attribute source '" + name
				+ "': connecting to <" + xuudbURL
				+ ">");
		initCache();
	}

	public void setXuudbPort(int port) {
		this.port = port;
	}

	public void setXuudbHost(String host) {
		this.host = host;
	}

	public void setXuudbGCID(String gcID) {
		this.gcID = gcID;
	}

	public void setXuudbCache(boolean cache) {
		this.cacheCredentials = cache;
	}

	protected String getXUUDBUrl() {
		return xuudbURL;
	}

	protected void initCache() {
		cache = new CredentialCache();
	}

	protected boolean isNotEmpty(String s) {
		return s != null && !s.isEmpty();
	}


	// monitoring information and query hooks

	private Set<String> accessorNames = new HashSet<String>();

	public String[] getRequestorNames() {
		return accessorNames.toArray(new String[accessorNames.size()]);
	}

	public void clearRequestorNames() {
		accessorNames.clear();
	}

	public void addAccessorName(String name) {
		boolean newEntry = accessorNames.add(name);
		if (newEntry) {
			logger.info("New client: " + name);
		}
	}

	public void clearCache() {
		cache.removeAll();
	}

	public long getCacheSize() {
		return cache.getCacheSize();
	}

	public boolean getCachingCredentials() {
		return cacheCredentials;
	}

	public void toggleCachingCredentials() {
		cacheCredentials = !cacheCredentials;
	}
	
	protected void updateXUUDBConnectionStatus() {
		if (!isEnabled){
			statusMessage = "Not enabled";
			status = Status.NOT_APPLICABLE;
			return;
		}
		
		// TODO - insecure mode won't work here
		Callable<X509Certificate[]> getCert = new Callable<X509Certificate[]>() {
			public X509Certificate[] call() throws Exception {
				return ConnectionUtil.getPeerCertificate(kernel.getClientConfiguration(), 
						getXUUDBUrl(), 5000, logger);
			}
		};
		ThreadingServices ts = kernel.getContainerProperties()
				.getThreadingServices();
		if (TimeoutRunner.compute(getCert, ts, 2000) != null) {
			statusMessage = "OK [" + name
					+ " connected to " + getXUUDBUrl() + "]";
			status = Status.OK;
			cb.OK();
		}
		else{
			statusMessage = "CAN'T CONNECT TO XUUDB";
			status = Status.DOWN;
			cb.notOK(statusMessage);
		}
	}
	
	public String getXUUDBConnectionStatus() {
		updateXUUDBConnectionStatus();
		return statusMessage;
	}

	public String getStatusDescription() {
		return getXUUDBConnectionStatus();
	}

	public String getName() {
		return name;
	}

	public String getConnectionStatusMessage(){
		return getXUUDBConnectionStatus();
	}
	
	public Status getConnectionStatus(){
		return status;
	}
	
	/**
	 * simple name of the external system
	 */
	public String getExternalSystemName(){
		return name +" attribute source";
	}
	
}
