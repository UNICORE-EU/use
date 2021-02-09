package eu.unicore.services.ws.cxf;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Service;
import de.fzj.unicore.wsrflite.impl.DefaultHome;
import eu.unicore.util.Log;

public class CXFService implements Service {

	private static final Logger logger=Log.getLogger(Log.SERVICES, CXFService.class);

	public static final String TYPE="cxf";

	private final String name;

	private volatile boolean stopped=true;

	private Home home;

	private final Kernel kernel;

	private String frontend;

	private final DeploymentDescriptor serviceConfiguration;

	public CXFService(DeploymentDescriptor serviceConfiguration){
		this.name=serviceConfiguration.getName();
		this.kernel=serviceConfiguration.getKernel();
		this.frontend = serviceConfiguration.getFrontend()!=null?
				serviceConfiguration.getFrontend().getName() : null;
		this.serviceConfiguration=serviceConfiguration;
	}

	public final DeploymentDescriptor getServiceConfiguration() {
		return serviceConfiguration;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return TYPE;
	}

	public void start() throws Exception{
		if(!stopped){
			logger.debug("Not starting service <+"+name+"> : already started.");
			return;
		}
		Class<?>spec = serviceConfiguration.getInterface();
		Class<?>impl = serviceConfiguration.getImplementation();
		boolean isWSRF = isWSRF(impl);
		if(isWSRF){
			home = kernel.getHome(serviceConfiguration.getName());
			if(home==null){
				home=(Home)impl.getConstructor().newInstance();
				home.setKernel(kernel);
				home.activateHome(name);
				kernel.putHome(home);
			}
		}
		CXFKernel.get(kernel).exposeAsService(name,spec,impl,isWSRF);
		logger.debug("Started service <+"+name+">");
	}

	public void stop() {
		stopped = CXFKernel.get(kernel).unregisterService(getName());		
		if(home!=null){
			home.passivateHome();
		}
	}

	public void stopAndCleanup() {
		stop();
	}

	public Home getHome(){
		return home;
	}

	public void setHome(Home home){
		this.home=home;
	}

	/**
	 * check whether the service is started
	 */
	public boolean isStarted(){
		return !stopped;
	}

	@Override
	public String getInterfaceClass() {
		return serviceConfiguration.getInterface().getName();
	}

	public void setFrontend(String frontend){
		this.frontend = frontend;
	}

	public String getFrontend(){
		return frontend;
	}

	public static boolean isWSRF(Class<?> classImpl) {
		String nameImplSuperClass = null;
		String nameDefaultHome = DefaultHome.class.getName();
		while(!nameDefaultHome.equals(nameImplSuperClass)){
			classImpl = classImpl.getSuperclass();
			if(classImpl==null)return false;
			else{
				nameImplSuperClass = classImpl.getName();
			}
		}
		return true;
	}

}
