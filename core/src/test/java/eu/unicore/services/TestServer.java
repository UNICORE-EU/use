package eu.unicore.services;

import java.util.Properties;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;

import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.server.ContainerHttpServerProperties;
import eu.unicore.services.server.JettyServer;
import eu.unicore.services.testservice.MockService;
import eu.unicore.util.httpclient.HttpUtils;
import junit.framework.TestCase;

public class TestServer extends TestCase{

	public void testSingleService()throws Exception{
		Properties props = TestConfigUtil.getInsecureProperties();
		props.setProperty(ContainerProperties.PREFIX + 
				ContainerProperties.SERVER_HOST, "localhost");
		Kernel k=new Kernel(props);
		
		JettyServer server=new JettyServer(k, new ContainerHttpServerProperties(props));
		try {
			server.start();
			MockService s1 = new MockService("test", k);
			k.getDeploymentManager().deployService(s1);
			//fire a request at the service
			HttpClient client=HttpUtils.createClient(server.getUrls()[0].toString(), k.getClientConfiguration());
			HttpGet get=new HttpGet(server.getUrls()[0]+"/mock/test");
			client.execute(null, get, new BasicHttpClientResponseHandler());
			assertEquals(1, s1.getInvocationCount());
		} finally {
			server.stop();
		}
	}
}
