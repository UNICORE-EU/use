package eu.unicore.services.restclient.oidc;

import static java.net.StandardProtocolFamily.UNIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

public class TestOIDCAgentAuthN  {

	@Test
	public void testOIDCAgentAuthN() throws Exception {
		String path = "target/mock_agent_socket";
		try(MockAgent ma = new MockAgent(path)){
			new Thread(ma).start();
			Thread.sleep(100);
			var a = new OIDCAgentAuthN();
			a.setAgentProxy(new OIDCAgentProxy(path));
			var p = new Properties();
			p.setProperty("oidc-agent.account", "test");
			a.setProperties(p);
			var m = new HttpGet("https://test");
			a.addAuthenticationHeaders(m);
			var h = m.getHeader("Authorization");
			assertNotNull(h);
			assertEquals("Bearer some_access_token", h.getValue());
			a.lastRefresh = 0l;
			a.token = null;
			a.refreshTokenIfNecessary();
			assertTrue(a.lastRefresh>0);
			assertNotNull(a.token);
		}
	}

	public class MockAgent implements Runnable, AutoCloseable {

		private final String path;
		private volatile boolean stopped = false;
		
		public MockAgent(String path) {
			this.path = path;
		}

		@Override
		public void run() {
			try {
				UnixDomainSocketAddress add = UnixDomainSocketAddress.of(path);
				System.out.println("Starting MockOIDCAgent listening on <"+path+">");
				ServerSocketChannel srv = ServerSocketChannel.open(UNIX);
				srv.bind(add);
				while(!stopped) {
					SocketChannel ch = srv.accept();
					if(ch!=null) {
						handle(ch);
					}
					Thread.sleep(100);
				}
				System.out.println("MockOIDCAgent exiting.");
				FileUtils.delete(new File(path));
			}catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void close() throws Exception {
			this.stopped = true;
			Thread.sleep(100);
		}
		
		private void handle(SocketChannel channel) throws Exception {
			try(OutputStream os = Channels.newOutputStream(channel);
					InputStream is = Channels.newInputStream(channel))
			{
				JSONObject in = new JSONObject(new JSONTokener(is));
				System.out.println("MockAgent <-- "+in.toString());
				JSONObject j = new JSONObject();
				j.put("status", "success");
				j.put("access_token", "some_access_token");
				j.put("refresh_token", "some_refresh_token");
				String res = j.toString();
				IOUtils.write(res, os, "UTF-8");
				System.out.println("MockAgent --> "+res);
			}
		}
	}
}