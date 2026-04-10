package eu.unicore.services.aip.xuudb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

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
import eu.unicore.util.Pair;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ConnectionUtil;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Base for XUUDB attribute sources
 *
 * @author schuller
 * @author golbi
 */
public abstract class XUUDBBase<T> implements IAttributeSourceBase, ExternalSystemConnector {

	protected static final Logger logger = Log.getLogger(Log.SECURITY, XUUDBBase.class);

	public static final int DEFAULT_PORT = 34463;
	public static final String DEFAULT_HOST = "https://localhost";

	protected String name;
	protected Integer port = DEFAULT_PORT;
	protected String host = DEFAULT_HOST;
	protected Boolean cacheCredentials = Boolean.TRUE;
	protected Kernel kernel;
	protected T xuudb;

	protected final CircuitBreaker cb = new CircuitBreaker();

	protected String xuudbURL = null;
	protected String gcID;
	protected CredentialCache cache;

	protected volatile Status status = Status.UNKNOWN;
	protected volatile String statusMessage = "N/A";
	private volatile long lastChecked;

	@Override
	public void configure(String name, Kernel kernel) throws ConfigurationException {
		this.name = name;
		this.kernel = kernel;
		setupURL();
		logger.info("Attribute source '{}': connecting to <{}>", name, getXUUDBUrl());
		initCache();
		xuudb = createEndpoint();
	}

	protected abstract T createEndpoint() throws ConfigurationException;

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

	long getCacheSize() {
		return cache.getCacheSize();
	}

	final AtomicBoolean pingInProgress = new AtomicBoolean(false);

	protected void updateXUUDBConnectionStatus() {
		if (pingInProgress.get() || (lastChecked+60000>System.currentTimeMillis()))
			return;
		try {
			pingInProgress.set(true);
			Pair<Boolean, String> res = checkXUUDBAlive();
			if (res.getM1()) {
				statusMessage = "OK [" + name + " " + res.getM2() + "]";
				status = Status.OK;
				cb.OK();
			}
			else{
				statusMessage = "CAN'T CONNECT TO XUUDB: "+res.getM2();
				status = Status.DOWN;
				cb.notOK();
			}
		} finally {
			pingInProgress.set(false);
		}
	}

	protected Pair<Boolean, String> checkXUUDBAlive() {
		final boolean isSecure = getXUUDBUrl().toLowerCase().startsWith("https");
		String msg = "connected to " + getXUUDBUrl();
		Boolean success = Boolean.TRUE;
		Callable<String> ping = isSecure ?
				()-> {
						ConnectionUtil.getPeerCertificate(kernel.getClientConfiguration(),
						getXUUDBUrl(), 5000, logger);
						return "OK";
				} :
				()-> {
					String h = host.split("://")[1];
					new Socket(InetAddress.getByName(h), port).close();
					return "OK";
				};
		ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
		try{
			if(TimeoutRunner.compute(ping, ts, 5100)== null) {
				msg = "timeout";
				success = Boolean.FALSE;
			}
		}catch(Exception e) {
			msg = Log.getDetailMessage(e);
			success = Boolean.FALSE;
		}
		return new Pair<>(success, msg);
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

	@Override
	public String toString() {
		return getName()+" ["+getXUUDBUrl()+"]";
	}

	@Override
	public void reloadConfig(Kernel kernel) throws Exception {
		xuudb = createEndpoint();
	}

}
