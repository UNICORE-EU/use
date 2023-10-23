package eu.unicore.services.rest;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Service;
import eu.unicore.services.ServiceFactory;
import eu.unicore.services.exceptions.ServiceDeploymentException;
import jakarta.ws.rs.core.Application;

public class RestServiceFactory implements ServiceFactory {
	
	private static RestServlet servlet;
	
	public Service build(DeploymentDescriptor dd)throws ServiceDeploymentException{
		try{
			RestService service=new RestService(dd.getName(),dd.getKernel());
			Class<?>applicationClass = dd.getImplementation();
			Application application=(Application)applicationClass.getConstructor().newInstance();
			service.setApplication(application);
			return service;
		}catch(Exception ex){
			throw new ServiceDeploymentException("Error deploying service '"+dd.getName()+"'",ex);
		}
		
	}

	public String getType() {
		return RestService.TYPE;
	}

	public String getServletClass() {
		return RestServlet.class.getName();
	}

	public String getServletPath() {
		return "/rest/*";
	}

	public synchronized RestServlet getServlet(){
		if(servlet==null){
			servlet=new RestServlet();
		}
		return servlet;
	}

}
