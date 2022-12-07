package eu.unicore.services.rest;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpTransport;
import org.eclipse.jetty.server.Request;

import eu.unicore.util.jetty.forwarding.Forwarder;
import eu.unicore.util.jetty.forwarding.ForwardingConnection;
import eu.unicore.util.jetty.forwarding.UpgradeHttpServletRequest;
import eu.unicore.util.jetty.forwarding.UpgradeHttpServletResponse;
import eu.unicore.util.Log;

public class RestServlet extends CXFNonSpringServlet {
	
	private static final long serialVersionUID=1l;

	private static final Logger logger = Log.getLogger(Log.SERVICES, RestServlet.class);

	public static ThreadLocal<SocketChannel> backends = new ThreadLocal<>();

	@Override
	protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		super.invoke(request, response);
		if(response.getStatus()==101) {
			logger.info("Handling 101 Switching Protocols");
			SocketChannel backend = backends.get();
			if(backend==null)throw new ServletException("No back-end for forwarding found.");
			backends.remove();
			try{
				upgradeConnection(request, response, backend);
			}catch(Exception ex) {
				throw new ServletException(ex);
			}
		}
	}

	protected void upgradeConnection(HttpServletRequest request, HttpServletResponse response, SocketChannel backend) throws Exception {
		Request baseRequest = Request.getBaseRequest(request);
		ForwardingConnection toClient = createForwardingConnection(baseRequest, backend);
		if (toClient == null)
			throw new IOException("not upgraded: no connection");
		logger.debug("forwarding-connection {}", toClient);
		HttpChannel httpChannel = baseRequest.getHttpChannel();
		httpChannel.getConnector().getEventListeners().forEach(toClient::addEventListener);
		baseRequest.setHandled(true);
		baseRequest.setAttribute(HttpTransport.UPGRADE_CONNECTION_ATTRIBUTE, toClient);

		// Save state from request/response and remove reference to the base request/response.
		new UpgradeHttpServletRequest(request).upgrade();
		new UpgradeHttpServletResponse(response).upgrade();
		Forwarder.get().attach(toClient);
		
		logger.info("Forwarding from backend {}, client={}", backend, toClient.getEndPoint().getRemoteSocketAddress());
	}
	
	protected ForwardingConnection createForwardingConnection(Request baseRequest, SocketChannel backend) {
		HttpChannel httpChannel = baseRequest.getHttpChannel();
		Connector connector = httpChannel.getConnector();
		return new ForwardingConnection(httpChannel.getEndPoint(),
				connector.getExecutor(),
				backend);
	}
}
