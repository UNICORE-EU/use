package eu.unicore.services.ws.testutils;


import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.canl.DefaultAuthnAndTrustConfiguration;
import eu.unicore.security.wsutil.AuthInHandler;
import eu.unicore.security.wsutil.ConditionalGetServerInHandler;
import eu.unicore.security.wsutil.ConditionalGetServerOutHandler;
import eu.unicore.security.wsutil.DSigParseInHandler;
import eu.unicore.security.wsutil.DSigSecurityInHandler;
import eu.unicore.security.wsutil.ETDInHandler;
import eu.unicore.security.wsutil.SecuritySessionCreateInHandler;
import eu.unicore.security.wsutil.SecuritySessionStore;
import eu.unicore.security.wsutil.SessionIDServerOutHandler;
import eu.unicore.security.wsutil.client.WSClientFactory;

/**
 * @author schuller
 * @author golbi
 */
public abstract class AbstractTestBase 
{
	public static final String BASE_URL = "https://localhost:64345";
	
	public static final String KS = "src/test/resources/conf/server.jks";
	public static final String KS_PWD = "the!server";

	protected static JettyServer jetty; 
	protected static List<Server> cxfServices;
	protected static List<String> cxfServiceNames;
	protected static CXFNonSpringServlet servlet;
	protected static DefaultAuthnAndTrustConfiguration secConfiguration;
	

	protected final static String mockServiceName="MockServiceImpl";
	protected final static QName mockServiceQName=new QName("foo", mockServiceName);

	@BeforeClass
	public static void startServer() throws Exception
	{
		cxfServices = new ArrayList<Server>();
		cxfServiceNames = new ArrayList<String>();
		secConfiguration = new DefaultAuthnAndTrustConfiguration();
		secConfiguration.setCredential(new KeystoreCredential(KS, 
				KS_PWD.toCharArray(), 
				KS_PWD.toCharArray(), 
				null, 
				"JKS"));
		secConfiguration.setValidator(new KeystoreCertChainValidator(KS, 
				KS_PWD.toCharArray(), 
				"JKS", 
				-1));
		
		servlet=new CXFNonSpringServlet();
		jetty = new JettyServer(servlet, secConfiguration);
		jetty.start();
	
		addMockService();
	}
	
	@AfterClass
	public static void shutdownServer()throws Exception{
		Thread.sleep(1000);
		for(Server s: cxfServices){
			s.stop();
		}
		try{
			Thread.sleep(1000);
			jetty.stop();
		}catch(Exception ex){ex.printStackTrace();}
	}
	
	private static void addMockService()throws Exception{
		addService(mockServiceName, mockServiceQName, MockServiceImpl.class);
	}
	
	
	protected static void addService(String name, QName qname, Class<?>clazz){
		if(cxfServiceNames.contains(name)){
			return;
		}
		
		JaxWsServerFactoryBean factory=new JaxWsServerFactoryBean();
		factory.setServiceClass(clazz);
		factory.setBus(servlet.getBus());
		factory.setEndpointName(qname);
		factory.setServiceName(qname);
		factory.setAddress("/"+name);
		factory.setDataBinding(WSClientFactory.getBinding(clazz));
		factory.setStart(false);
		
		List<Interceptor<? extends Message>> s = factory.getInInterceptors();
		
		SecuritySessionStore sesStore = new SecuritySessionStore();
		AuthInHandler authHandler = new AuthInHandler(true, true, true, null, sesStore);
		DSigParseInHandler parseHandler = new DSigParseInHandler(null);
		DSigSecurityInHandler dsigHandler = new DSigSecurityInHandler(null);
		ETDInHandler etdHandler=new ETDInHandler(null, secConfiguration.getValidator(), null);
		SecuritySessionCreateInHandler sesHandler = new SecuritySessionCreateInHandler(sesStore);
		
		//WS-A stuff
		
		MAPAggregator mapAggregator=new MAPAggregator();
		MAPCodec mapCodec=new MAPCodec();
		
		s.add(mapAggregator);
		s.add(mapCodec);
		
		// security stuff
		s.add(authHandler);
		s.add(parseHandler);
		s.add(dsigHandler);
		s.add(etdHandler);
		s.add(sesHandler);
		s.add(new ConditionalGetServerInHandler());
		
		// out handlers
		List<Interceptor<? extends Message>> out = factory.getOutInterceptors();
		out.add(new ConditionalGetServerOutHandler());
		out.add(mapAggregator);
		out.add(mapCodec);
		out.add(new SessionIDServerOutHandler());
		
		Server srv=factory.create();
		srv.start();
		cxfServices.add(srv);
		cxfServiceNames.add(name);
	}

}
