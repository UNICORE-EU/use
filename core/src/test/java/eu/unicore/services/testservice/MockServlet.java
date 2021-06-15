package eu.unicore.services.testservice;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;

/**
 * Mock servlet servicing requests to {@link MockService} instances
 *
 */
public class MockServlet extends HttpServlet implements KernelInjectable{

	private final static long serialVersionUID=1L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		String serviceName = getServiceName(request);
		MockService service=(MockService)kernel.getService(serviceName);
		if(service==null){
			response.sendError(404, "The requested service '"+serviceName+"' cannot be found.");
			return;
		}
		service.invoke();
	}

	/**
	 * Get the service that is mapped to the specified request.
	 */
	protected String getServiceName(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();

		if (pathInfo == null)
			return null;

		String serviceName;

		if (pathInfo.startsWith("/"))
		{
			serviceName = pathInfo.substring(1);
		}
		else
		{
			serviceName = pathInfo;
		}

		return serviceName;
	}
	
	private Kernel kernel;
	
	public void setKernel(Kernel k){
		this.kernel=k;
	}
}
