package de.fzj.unicore.wsrflite.testservice;

import javax.servlet.Servlet;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Service;
import de.fzj.unicore.wsrflite.ServiceFactory;

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
