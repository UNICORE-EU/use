/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package de.fzj.unicore.wsrflite.testutils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.security.auth.x500.X500Principal;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.HttpServerProperties;
import eu.unicore.util.jetty.JettyLogger;
import eu.unicore.util.jetty.JettyServerBase;



/**
 * Test Jetty server implementation. Here appropriate connectors are configured. 
 * @author K. Benedyczak
 */
public class JettyServer extends JettyServerBase
{
	public static final int PORT = 65344;
	public static final X500Principal SERVER_IDENTITY = new X500Principal(
			"CN=TestServer, OU=ICM, O=UW, L=Warsaw, ST=Unknown, C=PL");

	private final CXFNonSpringServlet servlet;
	
	public JettyServer(CXFNonSpringServlet servlet, IAuthnAndTrustConfiguration secConfiguration) 
	throws ConfigurationException
	{
		super(prepareUrls(), secConfiguration, getCfg(), JettyLogger.class);
		this.servlet=servlet;
		initServer();
	}

	private static URL[] prepareUrls()
	{
		try
		{
			return new URL[] {new URL("https://localhost:"+PORT),
					new URL("http://localhost:"+(PORT+1))};
		} catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static HttpServerProperties getCfg()
	{
		Properties p = new Properties();
		p.setProperty(HttpServerProperties.DEFAULT_PREFIX+HttpServerProperties.REQUIRE_CLIENT_AUTHN, "false");
		return new HttpServerProperties(p);
	}
	
	@Override
	protected Handler createRootHandler() throws ConfigurationException
	{
		ServletContextHandler root=new ServletContextHandler(getServer(), "/", ServletContextHandler.SESSIONS);
		root.addServlet(new ServletHolder(servlet), "/services/*");
		return root;
	}
}
