package eu.unicore.services.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

/**
 * "best practices" implementation of the ExternalSystemConnector
 */
public class ExternalConnectorHelper implements ExternalSystemConnector {

	private static final Logger logger = Log.getLogger(Log.SERVICES, ExternalSystemConnector.class);

	private String externalSystemName;
	protected volatile Status status = Status.OK;
	protected volatile String statusMessage = "N/A";
	private volatile long lastChecked;
	private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
	private Callable<Pair<Boolean, String>> checkSupplier;
	private ExecutorService checkService;
	private long updateInterval = 60000;

	/**
	 * The checkSupplier provides the actual status checking code. This check
	 * should take appropriate precautions as to timeouts, and not hang forever.
	 * @param checkSupplier
	 */
	public void setCheckSupplier(Callable<Pair<Boolean, String>> checkSupplier) {
		this.checkSupplier = checkSupplier;
	}

	public void setExternalSystemName(String externalSystemName) {
		this.externalSystemName = externalSystemName;
	}

	public void setCheckService(ExecutorService checkService) {
		this.checkService = checkService;
	}

	@Override
	public String getConnectionStatusMessage() {
		runConnectionStatusUpdate();
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus() {
		runConnectionStatusUpdate();
		return status;
	}

	@Override
	public String getExternalSystemName() {
		return externalSystemName;
	}

	protected void setUpdateInterval(long interval) {
		this.updateInterval = interval;
	}

	/**
	 * Check if the service is useable, to the best of our knowledge
	 */
	public boolean isOK() {
		if(Status.OK != status){
			// if waiting period has passed, we reset the state to "OK"
			if(!checkInProgress.get() && lastChecked+updateInterval<System.currentTimeMillis()) {
				status = Status.OK;
				statusMessage = "OK";
			}
		}
		return Status.OK == status;
	}

	/**
	 * allows external users of the class to report a problem with the service
	 * 
	 * @param errorMessage
	 */
	public void notOK(String errorMessage) {
		if(!checkInProgress.get()) {
			status = Status.DOWN;
			statusMessage = errorMessage;
			lastChecked = System.currentTimeMillis();
		}
	}

	private volatile Future<?> resultGetter = null;

	// triggers a status update, if none is in progress and the waiting period
	// since the last run has passed
	protected void runConnectionStatusUpdate() {
		if (checkInProgress.get() || lastChecked+updateInterval>System.currentTimeMillis()) {
			return;
		}
		checkInProgress.set(true);
		try {
			Runnable r = () ->
			{
				Status oldStatus = status;
				try {
					logger.trace("Entering status check for <{}>, async={}", externalSystemName, checkService!=null);
					Pair<Boolean,String>result = checkSupplier.call();
					statusMessage = result.getM2();
					status = result.getM1()? Status.OK : Status.DOWN;
				}catch(Exception e) {
					notOK(Log.getDetailMessage(e));
				}
				finally {
					lastChecked = System.currentTimeMillis();
					checkInProgress.set(false);
					resultGetter = null;
				}
				Level lvl = status!=oldStatus? Level.INFO : Level.TRACE;
				logger.log(lvl, "<{}> is <{}> ({})", externalSystemName, status, statusMessage);
			};
			if(checkService!=null) {
				resultGetter = checkService.submit(()->{
					r.run();
				});
			}
			else {
				FutureTask<String> task = new FutureTask<String>(r, "OK");
				resultGetter = task;
				task.run();
			}
		}catch(Exception e) {
			notOK(Log.getDetailMessage(e));
			checkInProgress.set(false);
			resultGetter = null;
		}
	}

	@Override
	public void awaitConnectionStatusRefresh(long timeout, TimeUnit units) {
		runConnectionStatusUpdate();
		if(checkInProgress.get()) {
			try{
				while(resultGetter==null)Thread.sleep(50);
				if(resultGetter!=null)resultGetter.get(timeout, units);
			}catch(Exception te) {}
		}
	}
 
	public static void checkServerConnect(String host, int port, int timeout) throws Exception {
		try(Socket s = new Socket()){
			s.connect(new InetSocketAddress(host, port), timeout);
		}
	}

	/**
	 * helper to check an SSL connection
	 *
	 * @param securityCfg
	 * @param host
	 * @param port
	 * @param timeout in milliseconds
	 * @return
	 * @throws Exception
	 */
	public static X509Certificate[] getSSLPeer(IAuthnAndTrustConfiguration securityCfg, String host, int port, int timeout) throws Exception {
		SSLSocket s = null;
		try {
			SSLSocketFactory socketFactory = new SocketFactoryCreator2(securityCfg.getCredential(), 
					securityCfg.getValidator(), null).getSocketFactory();
			s = (SSLSocket) socketFactory.createSocket();
			s.connect(new InetSocketAddress(host, port), timeout);
			s.setSoTimeout(timeout);
			return CertificateUtils.convertToX509Chain(s.getSession().getPeerCertificates());
		}finally {
			IOUtils.closeQuietly(s);
		}
	}

	/**
	 * helper to check an SSL connection
	 *
	 * @param securityCfg
	 * @param url
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static X509Certificate[] getSSLPeer(IAuthnAndTrustConfiguration securityCfg, String url, int timeout) throws Exception {
		URL u = new URL(url);
		int port = u.getPort();
		if (port == -1)port = u.getDefaultPort();
		if (port == -1)port = 443;
		return getSSLPeer(securityCfg, u.getHost(), port, timeout);
	}

}
