package eu.unicore.uas.security.xuudb;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.logging.log4j.Logger;

import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ConnectionUtil;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Base for XUUDB attribute sources
 *
 * @author schuller
 * @author golbi
 */
public abstract class XUUDBBase implements IAttributeSourceBase, ExternalSystemConnector {

	protected static final Logger logger = Log.getLogger(Log.SECURITY, XUUDBBase.class);

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
		cb = new CircuitBreaker("AttributeSource_"+name);
		if (port == null)
			port = DEFAULT_PORT;
		if (host == null)
			host = DEFAULT_HOST;
		setupURL();
		logger.info("Attribute source '{}': connecting to <{}>",
				name, getXUUDBUrl());
		initCache();
	}

	protected void setupURL() {
		xuudbURL = host + ":" + port + "/";
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

	private Set<String> accessorNames = new HashSet<>();

	public String[] getRequestorNames() {
		return accessorNames.toArray(new String[accessorNames.size()]);
	}

	public void clearRequestorNames() {
		accessorNames.clear();
	}

	public void addAccessorName(String name) {
		boolean newEntry = accessorNames.add(name);
		if (newEntry) {
			logger.info("New client: {}", name);
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
		String msg = checkXUUDBAlive();
		if (msg!= null) {
			statusMessage = "OK [" + name
					+ " "+msg + "]";
			status = Status.OK;
			cb.OK();
		}
		else{
			statusMessage = "CAN'T CONNECT TO XUUDB";
			status = Status.DOWN;
			cb.notOK(statusMessage);
		}
	}
	
	protected String checkXUUDBAlive() {
		Callable<X509Certificate[]> getCert = new Callable<>() {
			public X509Certificate[] call() throws Exception {
				return ConnectionUtil.getPeerCertificate(kernel.getClientConfiguration(), 
						getXUUDBUrl(), 5000, logger);
			}
		};
		ThreadingServices ts = kernel.getContainerProperties()
				.getThreadingServices();
		try{
			return TimeoutRunner.compute(getCert, ts, 2000) != null ? 
				"connected to " + getXUUDBUrl() : null;
		}catch(Exception ex) {
			return null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getConnectionStatusMessage(){
		updateXUUDBConnectionStatus();
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus(){
		return status;
	}

	@Override
	public String getExternalSystemName(){
		return name +" attribute source";
	}
	
	protected String doGet(String url) throws IOException {
		HttpClient hc = HttpUtils.createClient(url, kernel.getClientConfiguration());
		HttpGet get = new HttpGet(url);
		get.setHeader("Accept", "application/json");
		try(ClassicHttpResponse response = hc.executeOpen(null, get, HttpClientContext.create())){
			if(response.getCode()!=200)throw new IOException("HTTP error "+response.getCode());
			return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		}
	}
}
