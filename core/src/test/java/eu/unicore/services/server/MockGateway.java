package eu.unicore.services.server;

import java.net.URL;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import eu.unicore.security.canl.AuthnAndTrustProperties;
import eu.unicore.util.jetty.HttpServerProperties;
import eu.unicore.util.jetty.JettyServerBase;

public class MockGateway extends JettyServerBase {

	public MockGateway(URL url, AuthnAndTrustProperties security, HttpServerProperties props) {
		super(url, security, props);
		initServer();
	}

	@Override
	protected Handler createRootHandler() {
		ServletContextHandler root = new ServletContextHandler(getServer(), "/", ServletContextHandler.SESSIONS);
		root.addServlet(new ServletHolder(DefaultServlet.class), "/");
		return root;
	}

}
