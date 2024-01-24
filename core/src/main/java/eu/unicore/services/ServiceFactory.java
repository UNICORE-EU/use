package eu.unicore.services;

import jakarta.servlet.Servlet;

/**
 * Service factories are used to build services of a certain types.
 * 
 * A service factory also contains information about the servlet that is
 * used to expose services of this type via Jetty. Either the servlet can
 * be given directly via getServlet(), or the servlet class can be given.
 * 
 * @author schuller
 */
public interface ServiceFactory {

	/**
	 * @return the type of service handled by this factory
	 */
	public String getType();
	
	/**
	 * build a service from the given configuration
	 */
	public Service build(DeploymentDescriptor configuration);
	
	
	/**
	 * Optionally, return the servlet instance for this service type
	 * @return Servlet instance or <code>null</code>.
	 */
	public Servlet getServlet();
		

	/**
	 * @return the servlet class name
	 */
	public String getServletClass();
	
	/**
	 * servlet path. Will be used to construct the address, e.g.
	 * https://host:port/servlet_path/service_name
	 */
	public String getServletPath();
	
}
