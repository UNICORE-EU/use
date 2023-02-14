package eu.unicore.services.rest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.Header;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.ChannelUtils;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.util.SSLSocketChannel;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Client-side helper class to establish port forwarding and handle
 * the subsequent traffic.
 *
 * @author schuller
 */
public class ForwardingHelper implements Runnable {

	protected static final Logger logger = Log.getLogger(Log.CLIENT, ForwardingHelper.class);

	public static final String REQ_UPGRADE_HEADER_VALUE = "UNICORE-Socket-Forwarding";

	private final BaseClient baseClient;

	/**
	 * create a new ForwardingHelper, taking security settings
	 * (authentication and user preferences)from the supplied BaseClient.
	 *
	 * @param baseClient - it is only used for handling authentication
	 *        and user preferences, not for the actual HTTP communication
	 * @throws IOException if the selector cannot be openend
	 */
	public ForwardingHelper(BaseClient baseClient) throws IOException {
		this.baseClient = baseClient;
		this.selector = Selector.open();
	}

	/**
	 * Open a port forwarding connection to the given endpoint
	 * and return the client-side SocketChannel for exchanging
	 * data with the backend service.
	 *
	 * The host/port of the backend service is either implicit
	 * (the endpoint knows what to do) or encoded into the endpoint
	 * using query parameters "?host=...&port=..."
	 *
	 * @param endpoint the URL to connect to.
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

	protected SocketChannel openSocketChannel(URI u) throws IOException {
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

	@SuppressWarnings("resource") // we do not want to close the channel of course
	protected void doHandshake(SocketChannel s, URI u, Header[] headers) throws Exception {
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
				throw new IOException("Endpoint <" + u.toString() +
						"> cannot handle UNICORE-Socket-Forwarding");
			}
			first = false;
		}
	}

	/**
	 * Start bi-directional data forwarding between the given two channels.
	 * (You can add more than one pair of channels)
	 *
	 * @param alice - one channel
	 * @param bob - the other channel
	 */
	public void startForwarding(SocketChannel alice, SocketChannel bob) throws IOException {
		attach(alice, bob);
		attach(bob, alice);
	}

	private final Selector selector;

	public void accept(final ServerSocketChannel server, final Consumer<SocketChannel> acceptHandler) throws IOException {
		server.register(selector, SelectionKey.OP_ACCEPT, acceptHandler);
	}

	public void attach(final SocketChannel source, final SocketChannel target)
			throws IOException {
		source.configureBlocking(false);
		target.configureBlocking(false);
		SocketChannel selectableSource = source instanceof SSLSocketChannel ?
				((SSLSocketChannel)source).getWrappedSocketChannel():
					source;
		Pair<Pair<SocketChannel,SocketChannel>, ByteBuffer> attachment = new Pair<>();
		attachment.setM1(new Pair<>(source, target));
		attachment.setM2(ByteBuffer.allocate(65536));
		selectableSource.register(selector, SelectionKey.OP_READ, attachment);
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try{
			while(selector.keys().size()>0) {
				selector.select(50);
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while(iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();
					if(key.isValid() && key.isReadable()) {
						dataAvailable(key);
					}
					else if(key.isValid() && key.isAcceptable()) {
						try {
							Consumer<SocketChannel> handler =
									(Consumer<SocketChannel>)key.attachment();
							ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
							SocketChannel client = ssc.accept();
							handler.accept(client);
						}catch(Exception ex) {
							logger.error(ex);
							key.cancel();
						}
					}
				}
			}
		}catch(Exception ex) {
			logger.error(ex);
		}
	}

	@SuppressWarnings("unchecked")
	protected void dataAvailable(SelectionKey key) {
		Pair<Pair<SocketChannel,SocketChannel>, ByteBuffer> attachment =
				(Pair<Pair<SocketChannel,SocketChannel>, ByteBuffer>)key.attachment();

		ByteBuffer buffer = attachment.getM2();
		Pair<SocketChannel,SocketChannel> channels = attachment.getM1();
		SocketChannel source = channels.getM1();
		SocketChannel target = channels.getM2();
		try{
			buffer.clear();
			int n = source.read(buffer);
			if(n>0) {
				buffer.flip();
				ChannelUtils.writeFully(target, buffer);
				logger.debug("{} bytes {} --> {}", n, source.getRemoteAddress(), target.getRemoteAddress());
			}
			else if(n==-1) {
				logger.debug("Source shutdown, closing.");
				IOUtils.closeQuietly(source);
				IOUtils.closeQuietly(target);
			}
		}catch(IOException ioe) {
			Log.logException("Error handling data move - closing.", ioe);
			IOUtils.closeQuietly(source);
			IOUtils.closeQuietly(target);
			key.cancel();
		}
	}

	public Selector getSelector() {
		return selector;
	}

}
