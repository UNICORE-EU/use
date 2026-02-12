package eu.unicore.services.rest;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletCoreRequest;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpStream;
import org.eclipse.jetty.server.Request;

import eu.unicore.util.Log;
import eu.unicore.util.jetty.forwarding.Forwarder;
import eu.unicore.util.jetty.forwarding.ForwardingConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestServlet extends CXFNonSpringServlet {

	private static final long serialVersionUID=1l;

	private static final Logger logger = Log.getLogger(Log.SERVICES, RestServlet.class);

	public static ThreadLocal<SocketChannel> backends = new ThreadLocal<>();

	@Override
	protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		super.invoke(request, response);
		if(response.getStatus()==101) {
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

	private void upgradeConnection(HttpServletRequest request, HttpServletResponse response, SocketChannel backend) throws Exception {
		Request baseRequest = ServletCoreRequest.wrap(request);
		ForwardingConnection toClient = createForwardingConnection(baseRequest, backend);
		if (toClient == null) {
			throw new IOException("not upgraded: no connection");
		}
		logger.debug("forwarding-connection {}", toClient);
		Connector connector = baseRequest.getConnectionMetaData().getConnector();
		connector.getEventListeners().forEach(toClient::addEventListener);
		baseRequest.setAttribute(HttpStream.UPGRADE_CONNECTION_ATTRIBUTE, toClient);
		Forwarder.get().attach(toClient);		
		logger.debug("Forwarding from backend {}, client={}", backend, toClient.getEndPoint().getRemoteSocketAddress());
	}
	
	protected ForwardingConnection createForwardingConnection(Request baseRequest, SocketChannel vsiteChannel)
	{
		Connector connector = baseRequest.getConnectionMetaData().getConnector();
		EndPoint ep  = baseRequest.getConnectionMetaData().getConnection().getEndPoint();
		return new ForwardingConnection(ep, connector.getExecutor(), vsiteChannel);
	}

}
