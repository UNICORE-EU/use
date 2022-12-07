package eu.unicore.services.rest.forwarding;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.RestServlet;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.ForwardingHelper;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.util.ChannelUtils;

public class TestPortForwardingEndpoint {

	Kernel kernel;
	static EchoServer echo;
	
	@Before
	public void startServer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty("container.host", "localhost");
		p.setProperty("container.port", "55333");
		p.setProperty("persistence.directory", "target/data");
		kernel=new Kernel(p);
		kernel.start();
		echo = new EchoServer();
		echo.start();
	}

	@After
	public void stopServer()throws Exception{
		kernel.shutdown();
		echo.shutdown();
	}

    @Test
	public void testInvokeForwardingService() throws Exception {
    	String sName="test";
		kernel.getContainerProperties().setProperty("messageLogging.test", "true");
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setKernel(kernel);
		dd.setType(RestService.TYPE);
		dd.setImplementation(MyApplication.class);
		dd.setName(sName);
		kernel.getDeploymentManager().deployService(dd);

		JettyServer server=kernel.getServer();
		String _url = server.getUrls()[0].toExternalForm()+"/rest/test/ports/test";
		BaseClient baseClient = new BaseClient(_url, kernel.getClientConfiguration());
		ForwardingHelper fh = new ForwardingHelper(baseClient);
		SocketChannel s = fh.connect(_url);
		PrintWriter w = new PrintWriter(new OutputStreamWriter(ChannelUtils.newOutputStream(s, 65536)), true);
		Reader r = new InputStreamReader(ChannelUtils.newInputStream(s, 65536));
		BufferedReader br = new BufferedReader(r);
		System.out.println("Forwarding established");
		String line = "this is a test";
		System.out.println("--> "+line);
		w.println(line);
		String reply = br.readLine();
		System.out.println("<-- "+reply);
		assertEquals(line, reply);
	}


	public static class MyApplication extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(MockResource.class);
			return classes;
		}
	}


	@Path("/")
	public static class MockResource {

		static final AtomicInteger invocationCounter=new AtomicInteger(0);
		
		@GET
		@Path("/ports/{name}")
		//@Produces("application/octet-stream")
		public Response forwardingEndpoint(@PathParam("name") String name, @HeaderParam("Upgrade") String upgrade){
			System.out.println("Incoming header: Upgrade: "+upgrade);
			ResponseBuilderImpl res = new ResponseBuilderImpl();
			res.status(HttpStatus.SWITCHING_PROTOCOLS_101);
			res.header("Upgrade", "UNICORE-Socket-Forwarding");
			try{
				RestServlet.backends.set(getBackend());
			}catch (Exception e) {
				throw new WebApplicationException(e.getMessage(), 500);
			}
			return res.build();
		}

	}

	public static SocketChannel getBackend() throws IOException {
		return SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), echo.getServerPort()));
	}
	
}
