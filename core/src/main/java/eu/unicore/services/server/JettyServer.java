/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package eu.unicore.services.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Decorator;

import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.ServiceFactory;
import eu.unicore.services.ThreadingServices;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.utils.FileWatcher;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.HttpServerProperties;
import eu.unicore.util.jetty.JettyServerBase;
import jakarta.servlet.Servlet;

/**
 * Jetty server for USE
 * 
 * @author schuller
 */
public class JettyServer extends JettyServerBase {
	private static final Logger logger = Log.getLogger(Log.UNICORE, JettyServer.class);

	private final Kernel kernel;
	
	protected static final HashMap<String, Integer> defaults = new HashMap<>();

	/**
	 * create a jetty server using settings from the supplied Kernel 
	 * @param kernel
	 * @param jettyCfg
	 * @throws Exception
	 */
	public JettyServer(Kernel kernel, HttpServerProperties jettyCfg) throws Exception {
		super(makeUrl(kernel), kernel.getContainerSecurityConfiguration(), jettyCfg);
		this.kernel = kernel;
		initServer();
	}

	private static URL makeUrl(Kernel kernel) throws MalformedURLException {
		int port = 0;
		String host = kernel.getContainerProperties().getValue(ContainerProperties.SERVER_HOST);
		String pString = kernel.getContainerProperties().getValue(ContainerProperties.SERVER_PORT);
		if (pString != null) port = Integer.parseInt(pString);
		
		if (kernel.getContainerSecurityConfiguration().isSslEnabled())
			return new URL("https://" + host + ":" + port);
		else
			return new URL("http://" + host + ":" + port);
	}

	/**
	 * After start, if a choice of listen port was left to the server,
	 * let's update listen port property. Also if externalUrl was not set explicitly, 
	 * let's update its port as it has (by default) the same port as the listen port.  
	 */
	@Override
	public void start() throws Exception {
		super.start();
		
		URL url = getUrls()[0];
		if ("0".equals(kernel.getContainerProperties().getValue(ContainerProperties.SERVER_PORT)))
			kernel.getContainerProperties().setProperty(ContainerProperties.SERVER_PORT, 
				String.valueOf(url.getPort()));
		
		String baseUrlS = kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
		URL baseUrl = new URL(baseUrlS);
		if (baseUrl.getPort() == 0 && baseUrl.getHost().equals(url.getHost())) {
			baseUrl = new URL(baseUrl.getProtocol(), baseUrl.getHost(), 
					url.getPort(), baseUrl.getFile());
			kernel.getContainerProperties().setProperty(ContainerProperties.EXTERNAL_URL, 
					baseUrl.toExternalForm());
		}
	}

	@SuppressWarnings("unchecked")
	protected void addServlets(ServletContextHandler root) throws ClassNotFoundException {
		for (ServiceFactory f: kernel.getServiceFactories()){
			
			Servlet servlet=f.getServlet();
			
			String servletClass=f.getServletClass();
			
			String path=f.getServletPath();
			String desc = servlet!=null? String.valueOf(servlet.getClass().getName()) : servletClass;
			
			if(servlet!=null){
				ServletHolder sh=new ServletHolder(servlet);
				root.addServlet(sh, path);
			}
			else{
				Class<?> loadedClazz = Class.forName(servletClass);
				if (!Servlet.class.isAssignableFrom(loadedClazz))
					throw new ConfigurationException("Class " + servletClass + " must extend Servlet class");
				ServletHolder sh=new ServletHolder((Class<? extends Servlet>)loadedClazz);
				root.addServlet(sh, path);
			}
			logger.debug("Added <{}> on {} for service type {}", desc, f.getType());
		}
	}

	public static class ServletDecorator implements Decorator {
		private final Kernel k;

		public ServletDecorator(Kernel k) {
			this.k = k;
		}

		@Override
		public <T> T decorate(T o) {
			if (o instanceof KernelInjectable) {
				((KernelInjectable) o).setKernel(k);
			}
			return o;
		}

		@Override
		public void destroy(Object o) {
			// NOP
		}
		
	}

	@Override
	protected Handler createRootHandler() throws ConfigurationException {
		ServletContextHandler root = new ServletContextHandler(getServer(), "/", ServletContextHandler.SESSIONS);
		root.getObjectFactory().addDecorator(new ServletDecorator(kernel));
		try {
			addServlets(root);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException("Can't add servlet, as " +
					"its class can not be loaded: " + e.getMessage(), e);
		}
		return root;
	}
	
	/**
	 * In USE we have always {@link ServletContextHandler} but it can be wrapped in {@link HandlerCollection}.
	 * @return the root servlet handler of this Jetty server. 
	 */
	public ServletContextHandler getRootServletContext() 
	{
		return (ServletContextHandler)super.getRootHandler();
	}

	@Override
	protected void initServer() throws ConfigurationException{
		super.initServer();
		final ContainerSecurityProperties secProps = kernel.getContainerSecurityConfiguration();
		if(secProps.isDynamicCredentialReloadEnabled()) {
			final CredentialProperties cProps = secProps.getCredentialProperties();
			String path = cProps.getValue(CredentialProperties.PROP_LOCATION);
			try{
				FileWatcher fw = new FileWatcher(new File(path), () -> {
					secProps.reloadCredential();
					reloadCredential();
				});
				ThreadingServices ts = kernel.getContainerProperties().getThreadingServices();
				ts.getScheduledExecutorService().scheduleWithFixedDelay(fw, 10, 10, TimeUnit.SECONDS);
			}catch(FileNotFoundException fe) {
				throw new ConfigurationException("", fe);
			}
		}
	}

	@Override
	public void reloadCredential() {
		super.reloadCredential();
		kernel.credentialReloaded();
	}
}
