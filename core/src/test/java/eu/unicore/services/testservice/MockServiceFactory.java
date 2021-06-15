package eu.unicore.services.testservice;

import javax.servlet.Servlet;

import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Service;
import eu.unicore.services.ServiceFactory;

public class MockServiceFactory implements ServiceFactory{

	public final Service build(DeploymentDescriptor configuration) {
		return new MockService(configuration.getName(),configuration.getKernel());
	}

	public final String getType() {
		return MockService.TYPE;
	}

	public final String getServletClass() {
		return MockServlet.class.getName();
	}

	public final String getServletPath() {
		return "/mock/*";
	}

	public Servlet getServlet(){
		return null;
	}
}
