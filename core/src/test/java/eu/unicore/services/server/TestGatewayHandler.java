package eu.unicore.services.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.security.canl.AuthnAndTrustProperties;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.server.GatewayHandler.GatewayRegistration;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.jetty.HttpServerProperties;

public class TestGatewayHandler {

	static MockGateway gw;
	static String gwUrl = "https://localhost:19191";

	@BeforeAll
	public static void startMockGateway() throws Exception {
		URL url = new URL(gwUrl);
		Properties props = new Properties();
		props.put("credential.path", "src/test/resources/conf/unicorex.p12");
		props.put("credential.password", "the!njs");
		props.put("truststore.type", "keystore");
		props.put("truststore.keystorePath", "src/test/resources/conf/truststore.jks");
		props.put("truststore.keystorePassword", "unicore");
		AuthnAndTrustProperties sec = new AuthnAndTrustProperties(props);
		HttpServerProperties hProps = new HttpServerProperties();
		gw = new MockGateway(url, sec, hProps);
		gw.start();
	}

	@AfterAll
	public static void stopMockGateway() {
		try{
			gw.stop();
		}catch(Exception ex) {}
	}

	@Test
	public void test1() throws Exception {
		Properties sp = new Properties();
		sp.put("container.security.credential.path", "src/test/resources/conf/unicorex.p12");
		sp.put("container.security.credential.password", "the!njs");
		sp.put("container.security.truststore.type", "keystore");
		sp.put("container.security.truststore.keystorePath", "src/test/resources/conf/truststore.jks");
		sp.put("container.security.truststore.keystorePassword", "unicore");
		sp.put("container.security.gateway.waitTime", "10");
		sp.put("container.security.gateway.registration", "true");
		sp.put("container.security.gateway.registrationSecret", "test123");

		Properties cp = new Properties();
		cp.put("container.externalurl", gwUrl+"/TEST");
		Properties clp = new Properties();
		clp.put("credential.path", "src/test/resources/conf/unicorex.p12");
		clp.put("credential.password", "the!njs");
		clp.put("truststore.type", "keystore");
		clp.put("truststore.keystorePath", "src/test/resources/conf/truststore.jks");
		clp.put("truststore.keystorePassword", "unicore");

		ContainerSecurityProperties csp = new ContainerSecurityProperties(sp);

		GatewayHandler gwh = new GatewayHandler(
				new ContainerProperties(cp, true), 
				new ClientProperties(clp),
				csp);
		assertTrue(csp.isGatewayWaitingEnabled());
		assertNull(csp.getGatewayCertificate());
		gwh.enableGatewayCertificateRefresh();
		gwh.waitForGateway();
		System.out.println(String.format("%s / %s",
				gwh.getName(),
				gwh.getStatusDescription()));
		assertNotNull(csp.getGatewayCertificate());
		assertTrue(gwh.getStatusDescription().startsWith("["));
		GatewayRegistration gwr = gwh.enableGatewayRegistration();
		assertNotNull(gwr);
		gwr.run();
	}
}