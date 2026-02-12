package eu.unicore.services.server;

import java.net.URL;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Handler;

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
		ServletContextHandler root = new ServletContextHandler("/", ServletContextHandler.SESSIONS);
		root.addServlet(new ServletHolder(DefaultServlet.class), "/");
		return root;
	}

}
