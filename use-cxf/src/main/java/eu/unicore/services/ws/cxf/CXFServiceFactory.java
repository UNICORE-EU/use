package eu.unicore.services.ws.cxf;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.ServiceFactory;
import de.fzj.unicore.wsrflite.exceptions.ServiceDeploymentException;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.ws.WSFrontEnd;

public class CXFServiceFactory implements ServiceFactory {

	public CXFService build(DeploymentDescriptor configuration)throws ServiceDeploymentException{
		try{
			CXFService service=new CXFService(configuration);
			return service;
		}catch(Exception ex){
			String name=configuration.getName();
			throw new ServiceDeploymentException("Error deploying service '"+name+"'",ex);
		}
		
	}

	public String getType() {
		return CXFService.TYPE;
	}

	public String getServletClass() {
		return Servlet.class.getName();
	}

	public String getServletPath() {
		return "/services/*";
	}

	private static Servlet servlet;
	
	public synchronized Servlet getServlet(){
		if(servlet==null){
			servlet=new Servlet();
		}
		return servlet;
	}
	/**
	 * Helper method to quickly generate a valid config for WS(RF) services
	 * @param name - the service name
	 * @param spec - the interface class
	 * @param impl - the implementation (or Home class in the WSRF case)
	 */
	public static DeploymentDescriptor generateServiceConfig(Kernel kernel, String name, Class<?>spec, Class<?>impl){
		DeploymentDescriptorImpl sc = new DeploymentDescriptorImpl();
		sc.setInterface(spec);
		sc.setImplementation(impl);
		sc.setKernel(kernel);
		sc.setName(name);
		return sc;
	}

	/**
	 * Helper method to quickly deploy a WS(RF) services
	 * 
	 * @param name - the service name
	 * @param spec - the interface class
	 * @param impl - the implementation (or Home class in the WSRF case)
	 * @param isPersistent - whether persistence should be enabled
	 * @param frontend - name of the {@link WSFrontEnd} class
	 */
	public static void createAndDeployService(Kernel kernel, String name, Class<?>spec, Class<?>impl, String frontend) throws Exception{
		DeploymentDescriptor c = CXFServiceFactory.generateServiceConfig(kernel, name, spec, impl);
		CXFService s=new CXFServiceFactory().build(c);
		s.setFrontend(frontend);
		kernel.getDeploymentManager().deployService(s);
	}
}
