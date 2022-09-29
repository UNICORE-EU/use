package eu.unicore.uas.security.saml.basic;


import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.canl.DefaultAuthnAndTrustConfiguration;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.HttpServerProperties;
import eu.unicore.util.jetty.JettyServerBase;



/**
 * Test Jetty server implementation. Here appropriate connectors are configured. 
 * @author K. Benedyczak
 */
public class JettyServer extends JettyServerBase
{
	public static final int PORT = 65344;
	public static final String KS = "src/test/resources/server.jks";
	public static final String KS_PWD = "the!server";
	private final CXFNonSpringServlet servlet;
	
	public JettyServer(CXFNonSpringServlet servlet) throws Exception
	{
		super(new URL("https://localhost:" + PORT), getSec(), getCfg());
		this.servlet = servlet;
		initServer();
	}

	private static IAuthnAndTrustConfiguration getSec() throws KeyStoreException, IOException
	{
		DefaultAuthnAndTrustConfiguration ret = new DefaultAuthnAndTrustConfiguration();
		ret.setCredential(new KeystoreCredential(KS, KS_PWD.toCharArray(), KS_PWD.toCharArray(), 
				null, "jks"));
		ret.setValidator(new KeystoreCertChainValidator(KS, KS_PWD.toCharArray(), "jks", -1));
		return ret;
	}
	
	private static HttpServerProperties getCfg()
	{
		HttpServerProperties ret = new HttpServerProperties();
		ret.setProperty(HttpServerProperties.FAST_RANDOM, "true");
		return ret;
	}
	
	@Override
	protected Handler createRootHandler() throws ConfigurationException {
		ServletContextHandler root = new ServletContextHandler(getServer(), "/", ServletContextHandler.SESSIONS);
		root.addServlet(new ServletHolder(servlet), "/services/*");
		return root;
	}
}
