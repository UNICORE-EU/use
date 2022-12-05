package eu.unicore.services.rest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.Header;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.httpclient.CustomSSLConnectionSocketFactory;
import eu.unicore.util.httpclient.EmptyHostnameVerifier;
import eu.unicore.util.httpclient.HttpUtils;

public class ForwardingHelper {

	protected static final Logger logger = Log.getLogger(Log.CLIENT, ForwardingHelper.class);

	public static final String REQ_UPGRADE_HEADER_VALUE = "UNICORE-Socket-Forwarding";

	private final BaseClient baseClient;

	public ForwardingHelper(BaseClient baseClient) {
		this.baseClient = baseClient;
	}

	public Socket connect(String endpoint) throws Exception {
		URI u = new URI(endpoint);
		Socket s = openSocket(u);
		HttpGet req = new HttpGet(endpoint);
		req.addHeader("Connection", "Upgrade");
		req.addHeader("Upgrade", REQ_UPGRADE_HEADER_VALUE);
		baseClient.addAuth(req);
		baseClient.addUserPreferences(req);
		doHandshake(s, u, req.getHeaders());
		return s;
	}

    protected Socket openSocket(URI u) throws IOException {
    	Socket s = SocketChannel.open(new InetSocketAddress(u.getHost(), u.getPort())).socket();
    	if("http".equalsIgnoreCase(u.getScheme())){
    		return s;
    	}
    	else if("https".equalsIgnoreCase(u.getScheme())) {
    		SSLContext sslc = HttpUtils.createSSLContext(baseClient.getSecurityConfiguration());
    		CustomSSLConnectionSocketFactory ssf = new CustomSSLConnectionSocketFactory(sslc, new EmptyHostnameVerifier());
    		return ssf.createLayeredSocket(s, u.getHost(), u.getPort(), null);
    	}
    	else throw new IOException();
    }

    protected void doHandshake(Socket s, URI u, Header[] headers) throws IOException {
    	PrintWriter out = new PrintWriter(s.getOutputStream());
		out.print("GET "+u.getPath()+" HTTP/1.1\r\n");
		logger.debug("--> GET {} HTTP/1.1", u.getPath());
		out.print("Host: "+u.getHost()+"\r\n");
		logger.debug("--> Host: {}", u.getHost());
		for(Header h: headers) {
			String line = h.getName()+": "+h.getValue();
			out.print(line+"\r\n");
			logger.debug("--> {}", line);
		}
		out.print("\r\n");
		out.flush();
		BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		boolean first = true;
		while(true) {
			String line = in.readLine();
			logger.debug("<-- {}", line);
			if(line==null || line.length()==0)break;
			if(first && !line.startsWith("HTTP/1.1 101")) {
				throw new IOException("Endpoint cannot handle UNICORE-Socket-Forwarding");
			}
			first = false;
		}
    }
    
}
