package eu.unicore.services.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;

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

	private String externalSystemName;
	protected volatile Status status = Status.UNKNOWN;
	protected volatile String statusMessage = "N/A";
	private volatile long lastChecked;
	private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
	private Callable<Pair<Boolean, String>> checkSupplier;
	private ExecutorService checkService;
	
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

	// triggers a status update, if none is in progress and the waiting period
	// since the last run has passed
	protected void runConnectionStatusUpdate() {
		if (checkInProgress.get() || lastChecked+60000>System.currentTimeMillis()) {
			return;
		}
		checkInProgress.set(true);
		try {
			Runnable r = () ->
			{
				try {
					Pair<Boolean,String>result = checkSupplier.call();
					statusMessage = result.getM2();
					status = result.getM1()? Status.OK : Status.DOWN;
				}catch(Exception e) {
					statusMessage = Log.getDetailMessage(e);
					status = Status.DOWN;
				}
			};
			if(checkService!=null) {
				checkService.submit(()->{
					r.run();
				});
			}
			else {
				r.run();
			}
		}catch(Exception e) {
			status = Status.DOWN;
			statusMessage = Log.getDetailMessage(e);
		}
		finally {
			lastChecked = System.currentTimeMillis();
			checkInProgress.set(false);
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
