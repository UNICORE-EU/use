/*********************************************************************************
 * Copyright (c) 2006-2012 Forschungszentrum Juelich GmbH 
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


package eu.unicore.services.ws.cxf;

import java.lang.reflect.Constructor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.trust.TruststoreBasedSamlTrustChecker;
import eu.unicore.security.wsutil.AuthInHandler;
import eu.unicore.security.wsutil.DSigParseInHandler;
import eu.unicore.security.wsutil.DSigSecurityInHandler;
import eu.unicore.security.wsutil.ETDInHandler;
import eu.unicore.security.wsutil.SecuritySessionCreateInHandler;
import eu.unicore.security.wsutil.SecuritySessionStore;
import eu.unicore.security.wsutil.SessionIDServerOutHandler;
import eu.unicore.security.wsutil.client.CheckUnderstoodHeadersHandler;
import eu.unicore.security.wsutil.cxf.XmlBeansDataBinding;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.UASSelfCallChecker;
import eu.unicore.services.security.UserAttributeCallback;
import eu.unicore.services.ws.WSFrontEnd;
import eu.unicore.services.ws.security.AccessControlHandler;
import eu.unicore.services.ws.security.UASDSigDecider2;
import eu.unicore.util.Log;

/**
 * Bridge to CXF
 * 
 * @author schuller
 */
public class CXFKernel {

	private static final Logger logger=Log.getLogger(Log.UNICORE, CXFKernel.class);

	private final Map<String,Server> services=Collections.synchronizedMap(new HashMap<String,Server>());

	private QName[] understoodHeaders=new QName[]{

	}; 
	
	//global handlers (i.e. those that are added to each service
	private boolean globalHandlersInitialized = false;
	private final List<Interceptor<?>> globalInHandlers = new ArrayList<Interceptor<?>>();
	private final List<Interceptor<?>> globalOutHandlers = new ArrayList<Interceptor<?>>();

	private final Kernel kernel;
	
	/**
	 * key for storing the name a service was deployed under in the Message properties
	 */
	public static final String SIMPLE_SERVICE_NAME="SIMPLE_SERVICE_NAME";
	
	private CXFKernel(Kernel kernel){
		this.kernel=kernel;
	}

	/**
	 * get the CXFKernel instance to be used with the given Kernel
	 * @param kernel - the Kernel
	 */
	public static synchronized CXFKernel get(Kernel kernel){
		CXFKernel xf=kernel.getAttribute(CXFKernel.class);
		if(xf==null){
			xf=new CXFKernel(kernel);
			kernel.setAttribute(CXFKernel.class, xf);
		}
		return xf;
	}
	
	/**
	 * setup and deploy a service
	 * @param name - service name
	 * @param spec - service interface class
	 * @param impl - service implementation class
	 * @param factory - factory to use. If null, a {@link JaxWsServerFactoryBean} will be created and used
	 * @param isWSRF - whether this is a WSRF service
	 * @param isPersistent - whether to enable persistent storage for the service
	 * @throws Exception
	 */
	public synchronized Service exposeAsService(String name, Class<?> spec, Class<?> impl, 
			JaxWsServerFactoryBean factory, boolean isWSRF, boolean isPersistent) throws Exception{
		JaxWsServerFactoryBean useFactory=factory;
		if(factory==null){
			useFactory = new JaxWsServerFactoryBean();
			useFactory.setBus(new CXFServiceFactory().getServlet().getBus());
		}

		if(useFactory.getDataBinding()==null){
			useFactory.setDataBinding(new XmlBeansDataBinding());
		}
		useFactory.setServiceClass(spec);
		
		if(isWSRF) {
			MAPAggregator mapAggregator=new MAPAggregator();
			MAPCodec mapCodec=new MAPCodec();
			
			useFactory.setInvoker(new WSRFInvoker(kernel));
			useFactory.getInInterceptors().add(mapAggregator);
			useFactory.getInInterceptors().add(mapCodec);
			useFactory.getInFaultInterceptors().add(mapAggregator);
			useFactory.getInFaultInterceptors().add(mapCodec);
			useFactory.getOutInterceptors().add(mapAggregator);
			useFactory.getOutInterceptors().add(mapCodec);
			useFactory.getOutFaultInterceptors().add(mapAggregator);
			useFactory.getOutFaultInterceptors().add(mapCodec);
		}
		else{
			useFactory.setInvoker(new PlainInvoker(kernel));
		}
		useFactory.getOutInterceptors().add(new SessionIDServerOutHandler());
		
		initializeGlobalHandlers();
		addHandlers(useFactory);

		if(isWSRF){
			useFactory.getInInterceptors().add(new WSRFHandler(kernel));
			useFactory.getInFaultInterceptors().add(new WSRFHandler(kernel));
		}
		
		useFactory.setAddress(name);
		
		Server server=useFactory.create();
		Service service=server.getEndpoint().getService();
		service.put(XmlBeansDataBinding.XMLBEANS_NAMESPACE_HACK, Boolean.TRUE);
		
		if(isWSRF){
			service.put(WSRFSERVICE,"true");
		}
		
		service.put(PlainInvoker.SERVICE_IMPL_CLASS, impl);
		service.put(SIMPLE_SERVICE_NAME,name);
		services.put(name,server);
		return service;
	}

	public Service exposeAsService(String name, Class<?> spec, Class<?> impl, 
			JaxWsServerFactoryBean factory, boolean isWSRF) throws Exception{
		return exposeAsService(name,spec,impl,factory,isWSRF,true);
	}

	public Service exposeAsService(String name, Class<?> spec, Class<?> impl, JaxWsServerFactoryBean factory)throws Exception{
		return exposeAsService(name,spec,impl,factory,true);
	}

	public Service exposeAsService(String name, Class<?> spec, Class<?> impl)throws Exception{
		return exposeAsService(name,spec,impl,null,true);
	}

	public Service exposeAsService(String name, Class<?> spec, Class<?> impl,boolean isWsrf)throws Exception{
		return exposeAsService(name,spec,impl,null,isWsrf);
	}

	/**
	 * Removes the Service object from the service map and stop it<br><br>
	 * @return boolean status flag
	 */
	public boolean unregisterService(String serviceName) {
		Server cxfService = services.remove(serviceName);
		cxfService.stop();
		return services.containsKey(serviceName);
	}

	public Collection<Server>getServices(){return services.values();}

	public Server getService(String name){return services.get(name);}

	/**
	 * support the SOAP mustUnderstand feature.
	 * Returns all headers we understand, such as wsaddressing, security, etc
	 */
	public QName[] getUnderstoodHeaders(){
		return understoodHeaders;
	}

	public synchronized void addUnderstoodHeaders(QName[] qn){
		QName[] qs=new QName[understoodHeaders.length+qn.length];
		for(int i=0;i<understoodHeaders.length;i++) qs[i]=understoodHeaders[i];
		for(int l=0;l<qn.length;l++) qs[understoodHeaders.length+l]=qn[l];
		understoodHeaders=qs;
	}

	private synchronized void initializeGlobalHandlers() {
		if (globalHandlersInitialized)
			return;
		
		globalHandlersInitialized = true;

		globalInHandlers.add(new CheckUnderstoodHeadersHandler());
		globalOutHandlers.add(new SessionIDServerOutHandler());
		
		boolean disableSig=!kernel.getContainerSecurityConfiguration().isSigningRequired();
		globalInHandlers.add(new DSigParseInHandler(new UASDSigDecider2(
				kernel.getSecurityManager().getSignatureChecker(),disableSig)));
		
		SecuritySessionStore securitySessionStore = kernel.getOrCreateSecuritySessionStore();
		globalInHandlers.add(createAuthInHandler(securitySessionStore));
//		//null argument means that it is required that body is signed. 
		globalInHandlers.add(new DSigSecurityInHandler(null));
		globalInHandlers.add(new ETDInHandler(new UASSelfCallChecker(kernel.getSecurityManager()), 
				kernel.getContainerSecurityConfiguration().getETDValidator(),
				kernel.getContainerSecurityConfiguration().getTrustedAssertionIssuers()));
		
		boolean sessionsEnabled=kernel.getContainerSecurityConfiguration().isSessionsEnabled();
		if (sessionsEnabled)
		{
			long sessionLifetime=kernel.getContainerSecurityConfiguration().getSessionLifetime()*1000;
			SecuritySessionCreateInHandler sessionHandler = new SecuritySessionCreateInHandler(
					securitySessionStore, sessionLifetime);
			globalInHandlers.add(sessionHandler);
		}
		globalInHandlers.add(new AccessControlHandler(kernel));

	}
	
	private synchronized void addHandlers(JaxWsServerFactoryBean factory) {
		for (Interceptor<?> handler: globalOutHandlers){
			factory.getOutInterceptors().add(handler);
		}
		
		for (Interceptor<?> handler: globalInHandlers){
			factory.getInInterceptors().add(handler);		
		}
	}

	private AuthInHandler createAuthInHandler(SecuritySessionStore securitySessionStore)
	{
		IContainerSecurityConfiguration secCfg = kernel.getContainerSecurityConfiguration(); 
		boolean verifyConsignor = secCfg.isGatewaySignatureCheckingEnabled();
		boolean useGwAssertions = true;
		X509Certificate gatewayCert = null;
		if (verifyConsignor){
			gatewayCert = secCfg.getGatewayCertificate();
			if (gatewayCert == null){
				logger.info("IMPORTANT! Can't retrieve gateway certificate, required" +
						"for gateway assertion verification. " +
						"Gateway-based authentication is totally turned OFF!");
				useGwAssertions = false;
			}
		} else{
			logger.info("IMPORTANT! Gateway assertions verification is turned OFF." +
				" This is OK only if your UNICORE/X installation is protected " +
				"by a firewall, and the only way of accessing it is through the gateway." +
				" If this is not true then you SHOULD turn on the gateway assertion " +
				"verification to prevent unauthorized access.");
		}
		//verify signature only if configured to do so
		if(!verifyConsignor) gatewayCert=null;
		
		AuthInHandler h=new AuthInHandler(useGwAssertions, true, false, gatewayCert, securitySessionStore);
		h.addUserAttributeHandler(new UserAttributeCallback());
		
		if (secCfg.getCredential() != null)
		{
			String consumerIdentity = secCfg.getCredential().getSubjectName();
			TruststoreBasedSamlTrustChecker samlTrustChecker = new TruststoreBasedSamlTrustChecker(
				secCfg.getTrustedAssertionIssuers());
			String consumerEndpointUri = kernel.getContainerProperties().getBaseUrl();
			List<String>additionalSAMLNames = ((ContainerSecurityProperties)kernel.getContainerSecurityConfiguration()).
					getAdditionalSAMLIds();
			if(additionalSAMLNames.size()>0){
				logger.info("Accepted additional SAML service identifiers: "+additionalSAMLNames);
			}
			h.enableSamlAuthentication(consumerIdentity, consumerEndpointUri, 
				samlTrustChecker, IContainerSecurityConfiguration.SAML_VALIDITY_GRACE_TIME, additionalSAMLNames);
		}
		return h;
	}
	
	public synchronized void addGlobalInHandler(Interceptor<?> handler) {
		globalInHandlers.add(handler);
		Collection<Server> servicesCollection = services.values();
		for (Server service: servicesCollection) {
			service.getEndpoint().getInInterceptors().add(handler);
		}
	}

	public synchronized void addGlobalOutHandler(Interceptor<?> handler) {
		globalOutHandlers.add(handler);
		Collection<Server> servicesCollection = services.values();
		for (Server service: servicesCollection) {
			service.getEndpoint().getOutInterceptors().add(handler);
		}
	}
	
	public static final String WSRFSERVICE="unicore.wsrflite.isWSRFservice";

	public static final String BINDING_JAXB="jaxb";

	public static final String BINDING_XMLBEANS="xmlbeans";
	
	public static String getSimpleServiceName(Service service){
		return (String)service.get(SIMPLE_SERVICE_NAME);
	}
	
	public static String getSimpleServiceName(Message message){
		return getSimpleServiceName(message.getExchange().getService());
	}
	
	public static void setSimpleServiceName(Service service, String name){
		service.put(SIMPLE_SERVICE_NAME,name);
	}

	@SuppressWarnings("deprecation")
	public static WSFrontEnd createFrontEnd(Resource resource) throws Exception {
		Object frontEnd;
		String serviceName = resource.getServiceName();
		Home home = resource.getHome();
		
		// option 1 - get from service
		CXFService service = (CXFService)home.getKernel().getService(serviceName);
		String className = null;
		if(service!=null){
			className = service.getFrontend();
		}
		
		// option 2 - get from resource model - deprecated
		if(className==null){
			className = resource.getModel().getFrontend(CXFService.TYPE);
		}
		
		if(!"self".equals(className) || !(resource instanceof WSFrontEnd)){
			frontEnd = findConstructor(Class.forName(className)).newInstance(resource);
		}
		else{
			frontEnd = resource;
		}
		return (WSFrontEnd)frontEnd;
	}
	
	private static Constructor<?> findConstructor(Class<?> clazz){
		for(Constructor<?>c:clazz.getConstructors()){
			if(c.getParameterTypes().length==1 && Resource.class.isAssignableFrom(c.getParameterTypes()[0])){
				return c;
			}
		}
		throw new IllegalStateException("Frontend class needs constructor with a single Resource argument");
	}
	
}
