package eu.unicore.services.rest;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.rest.impl.PostInvokeHandler;
import eu.unicore.services.rest.impl.USERestInvoker;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.rest.security.AuthenticatorChain;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.core.Application;

/**
 * a UNICORE REST service corresponds to a JAX-RS {@link Application}
 * 
 * @author schuller
 */
public class RestService implements Service {

	public static final String TYPE="rest";

	/**
	 * key for storing the name a service was deployed under in the Message properties
	 */
	public static final String SIMPLE_SERVICE_NAME="SIMPLE_SERVICE_NAME";

	private final String name;

	private volatile boolean stopped=true;

	private Application application;

	private Server cxfServer;
	private JAXRSServerFactoryBean bean;
	
	private final Kernel kernel;

	public RestService(String name, Kernel kernel){
		this.name = name;
		this.kernel = kernel;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void start()throws Exception{
		initSecurity();
		if(stopped){
			stopped = false;
			cxfServer = deploy();
			cxfServer.getEndpoint().getService().put(SIMPLE_SERVICE_NAME,name);
			if(application instanceof USERestApplication){
				try{
					((USERestApplication)application).initialize(kernel);
				}
				catch(Exception ex){
					cxfServer.destroy();
					throw ex;
				}
			}
		}
	}

	protected void initSecurity(){
		AuthenticatorChain.get(kernel);
	}

	@Override
	public void stop()throws Exception{
		if(!stopped){
			stopped=true;
			cxfServer.stop();
		}
	}

	@Override
	public void stopAndCleanup() throws Exception{
		stop();
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public Home getHome(){
		return null;
	}

	/**
	 * check whether the service is started
	 */
	public boolean isStarted(){
		return !stopped;
	}

	public String getInterfaceClass(){
		return null;
	}

	public Server deploy(){
		bean = ResourceUtils.createApplication(application, true, false, false, null);
		bean.setBus(new RestServiceFactory().getServlet().getBus());
		bean.setAddress("/"+name);
		boolean auth = application.getClass().getAnnotation(PermitAll.class)==null;
		if(auth)bean.setProvider(new AuthNHandler(kernel, kernel.getOrCreateSecuritySessionStore()));
		ContainerProperties sp = kernel.getContainerProperties();
		boolean enableLogging = sp.getBooleanValue(ContainerProperties.LOGGING_KEY+name);
		if(enableLogging) {
			bean.getFeatures().add(new LoggingFeature());
		}
		bean.getInFaultInterceptors().add(new PostInvokeHandler());
		bean.getOutFaultInterceptors().add(new PostInvokeHandler());
		bean.getOutInterceptors().add(new PostInvokeHandler());
		bean.setServiceName(new QName("", "test"));
		bean.setInvoker(new USERestInvoker(kernel));
		return bean.create();
	}

	public JAXRSServerFactoryBean getCXFServer() {
		return bean;
	}
}
