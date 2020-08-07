package eu.unicore.services.rest;

import javax.ws.rs.core.Application;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Service;
import eu.unicore.security.wsutil.client.LogInMessageHandler;
import eu.unicore.services.rest.impl.PostInvokeHandler;
import eu.unicore.services.rest.impl.USERestInvoker;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.rest.security.RESTSecurityProperties;

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

	private ClassLoader classLoader;
	
	private volatile boolean stopped=true;
	
	private Application application;
	
	private Server cxfServer;
	
	private final Kernel kernel;
	
	public RestService(String name, Kernel kernel){
		this.name = name;
		this.kernel = kernel;
	}
	
	
	public String getName() {
		return name;
	}

	public String getType() {
		return TYPE;
	}

	public void start()throws Exception{
		initSecurity();
		if(stopped){
			stopped=false;
			cxfServer=deploy();
			cxfServer.getEndpoint().getService().put(SIMPLE_SERVICE_NAME,name);
			// initialise application
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

	protected synchronized void initSecurity(){
		if(kernel.getAttribute(RESTSecurityProperties.class)==null){
			RESTSecurityProperties sp = new RESTSecurityProperties(kernel, kernel.getContainerProperties().getRawProperties());
			kernel.setAttribute(RESTSecurityProperties.class, sp);
			
		}
	}
	
	public void stop()throws Exception{
		if(!stopped){
			stopped=true;
			cxfServer.stop();
		}
	}

	public void stopAndCleanup() throws Exception{
		stop();

	}
	
	public ClassLoader getClassLoader(){
		return classLoader;
	}
	
	public void setClassLoader(ClassLoader cl){
		this.classLoader=cl;
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
		JAXRSServerFactoryBean bean = ResourceUtils.createApplication(application, true, false, false, null);
		bean.setBus(new RestServiceFactory().getServlet().getBus());
		bean.setAddress("/"+name);
		bean.setProvider(new AuthNHandler(kernel, kernel.getOrCreateSecuritySessionStore()));
		bean.getInInterceptors().add(new LogInMessageHandler());
		
		bean.getInFaultInterceptors().add(new PostInvokeHandler());
		bean.getOutFaultInterceptors().add(new PostInvokeHandler());
		bean.getOutInterceptors().add(new PostInvokeHandler());
		
		bean.setInvoker(new USERestInvoker(kernel));
		return bean.create();
	}

}