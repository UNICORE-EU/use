package eu.unicore.services.rest;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Service;
import eu.unicore.services.ServiceFactory;
import eu.unicore.util.configuration.ConfigurationException;
import jakarta.ws.rs.core.Application;

public class RestServiceFactory implements ServiceFactory {

	private static RestServlet servlet;

	@Override
	public Service build(DeploymentDescriptor dd) {
		RestService service=new RestService(dd.getName(),dd.getKernel());
		Class<?>applicationClass = dd.getImplementation();
		try {
			Application application=(Application)applicationClass.getConstructor().newInstance();
			service.setApplication(application);
		}catch(Exception e) {
			throw new ConfigurationException("Could not instantiate application class", e);
		}
		return service;
	}

	@Override
	public String getType() {
		return RestService.TYPE;
	}

	@Override
	public String getServletClass() {
		return RestServlet.class.getName();
	}

	@Override
	public String getServletPath() {
		return "/rest/*";
	}

	@Override
	public synchronized RestServlet getServlet(){
		if(servlet==null){
			servlet=new RestServlet();
		}
		return servlet;
	}

}
