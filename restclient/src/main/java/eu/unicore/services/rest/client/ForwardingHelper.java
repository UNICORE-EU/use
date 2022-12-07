package eu.unicore.services.rest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.Header;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.ChannelUtils;
import eu.unicore.util.Log;
import eu.unicore.util.SSLSocketChannel;
import eu.unicore.util.httpclient.HttpUtils;

public class ForwardingHelper {

	protected static final Logger logger = Log.getLogger(Log.CLIENT, ForwardingHelper.class);

	public static final String REQ_UPGRADE_HEADER_VALUE = "UNICORE-Socket-Forwarding";

	private final BaseClient baseClient;

	public ForwardingHelper(BaseClient baseClient) {
		this.baseClient = baseClient;
	}

	/**
	 * 
	 * @param endpoint
	 * @param serviceHost - the backend service to connect to
	 * @param servicePort - the backend service port to connect to
	 * @return SocketChannel
	 * @throws Exception
	 */
	public SocketChannel connect(String endpoint) throws Exception {
		URI u = new URI(endpoint);
		SocketChannel s = openSocketChannel(u);
		HttpGet req = new HttpGet(endpoint);
		req.addHeader("Connection", "Upgrade");
		req.addHeader("Upgrade", REQ_UPGRADE_HEADER_VALUE);
		baseClient.addAuth(req);
		baseClient.addUserPreferences(req);
		doHandshake(s, u, req.getHeaders());
		return s;
	}

	public SocketChannel openSocketChannel(URI u) throws Exception {
		SocketChannel s = SocketChannel.open(new InetSocketAddress(u.getHost(), u.getPort()));
		s.configureBlocking(false);
		if("http".equalsIgnoreCase(u.getScheme())){
			return s;
		}
		else if("https".equalsIgnoreCase(u.getScheme())) {
			SSLContext sslc = HttpUtils.createSSLContext(baseClient.getSecurityConfiguration());
    		SSLEngine sslEngine = sslc.createSSLEngine(u.getHost(), u.getPort());
			sslEngine.setUseClientMode(true);
			return new SSLSocketChannel(s, sslEngine, null);
		}
		else throw new IOException();
	}

    @SuppressWarnings("resource")
	public void doHandshake(SocketChannel s, URI u, Header[] headers) throws Exception {
		OutputStream os = ChannelUtils.newOutputStream(s, 65536);
		PrintWriter pw = new PrintWriter(os, true, Charset.forName("UTF-8"));
		String path = u.getPath();
		if(u.getQuery()!=null) {
			path += "?"+u.getQuery();
		}
		pw.format("GET %s HTTP/1.1\r\n", path);
		logger.debug("--> GET {} HTTP/1.1", path);
		pw.format("Host: %s\r\n", u.getHost());
		logger.debug("--> Host: {}", u.getHost());
		for(Header h: headers) {
			pw.format("%s: %s\r\n", h.getName(), h.getValue());
			logger.debug("--> {}: {}", h.getName(), h.getValue());
		}
		pw.format("\r\n");
		logger.debug("-->");
		InputStream is = ChannelUtils.newInputStream(s, 65536);
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		boolean first = true;
		String line=null;
		while( (line=in.readLine())!=null) {
			logger.debug("<-- {}", line);
			if(line.length()==0)break;
			if(first && line!=null && !line.startsWith("HTTP/1.1 101")) {
				throw new IOException("Backend site cannot handle UNICORE-Socket-Forwarding");
			}
			first = false;
		}
	}
        
}
