/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package eu.unicore.services.ws.client;

import static de.fzj.unicore.wsrflite.ContainerProperties.PREFIX;
import static de.fzj.unicore.wsrflite.ContainerProperties.WSRF_HOST;
import static de.fzj.unicore.wsrflite.ContainerProperties.WSRF_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.security.ContainerSecurityProperties;
import de.fzj.unicore.wsrflite.xmlbeans.AddTestResourceDocument;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.security.wsutil.client.SessionIDInHandler;
import eu.unicore.services.ws.exampleservice.MockPDP;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TestClientSSL extends TestClient{

	public static final String P2 = ContainerSecurityProperties.PREFIX;
	public static final String PT = TruststoreProperties.DEFAULT_PREFIX;
	public static final String PC = CredentialProperties.DEFAULT_PREFIX;
	public static final String PCS = ContainerSecurityProperties.PREFIX;

	//client side settings for SSL
	public static final String props = 
			PT+TruststoreProperties.PROP_KS_PATH+"=src/test/resources/conf/user-keystore.jks\n" +
					PT+TruststoreProperties.PROP_KS_PASSWORD+"=the!user\n" +
					PT+TruststoreProperties.PROP_TYPE+"=keystore\n" +
					PC+CredentialProperties.PROP_FORMAT+"=jks\n" +
					PC+CredentialProperties.PROP_LOCATION+"=src/test/resources/conf/user-keystore.jks\n" +
					PC+CredentialProperties.PROP_PASSWORD+"=the!user\n"+
					"client.serverHostnameChecking=NONE\n";

	//server side settings for SSL
	public static final String serverprops=
			P2+PT+TruststoreProperties.PROP_KS_PATH+"=src/test/resources/conf/keystore.jks\n" +
					P2+PT+TruststoreProperties.PROP_KS_PASSWORD+"=the!njs\n" +
					P2+PT+TruststoreProperties.PROP_TYPE+"=keystore\n" +
					P2+PC+CredentialProperties.PROP_FORMAT+"=jks\n" +
					P2+PC+CredentialProperties.PROP_LOCATION+"=src/test/resources/conf/keystore.jks\n" +
					P2+PC+CredentialProperties.PROP_PASSWORD+"=the!njs\n" +
					PCS+ContainerSecurityProperties.PROP_GATEWAY_AUTHN+"=false\n" +
					PCS+ContainerSecurityProperties.PROP_CHECKACCESS_PDP+"="+MockPDP.class.getName()+"\n"
					;

	@Override
	public String getBaseurl(){
		return "https://localhost:65321/services";
	}

	@Override
	protected IClientConfiguration getClientSideSecurityProperties() throws IOException{
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(props.getBytes()));
		ClientProperties ret = new ClientProperties(p);
		ret.setUseSecuritySessions(true);
		ret.getETDSettings().setExtendTrustDelegation(true);
		ret.getETDSettings().setReceiver(new X500Principal("CN=Demo UNICORE/X,O=UNICORE,C=EU"));
		ret.setMessageLogging(false);
		return ret;
	}

	@Override
	protected Properties getServerSideSecurityProperties() throws IOException{
		Properties properties = new Properties();
		properties.load(new ByteArrayInputStream(serverprops.getBytes()));
		FileUtils.deleteDirectory(new File("target","data"));
		properties.setProperty("persistence.directory", "target/data");
		properties.setProperty(PREFIX+WSRF_HOST, "localhost");
		properties.setProperty(PREFIX+WSRF_PORT, ""+getPort());
		return properties;
	}

	@Test
	public void testAccessControlCheck(){
		AddTestResourceDocument req=AddTestResourceDocument.Factory.newInstance();
		req.addNewAddTestResource();
		try{
			factoryClient.forbiddenAddTestResource(req);
		}catch(Exception ex){
			assertTrue(ex.getMessage().contains("Access denied"));
		}
	}

	@Test
	public void testSessionIDs()throws Exception{
		System.out.println(client.getResourcePropertyDocument());
		Calendar time=client.getCurrentTime();
		assertNotNull(time);
		System.out.println("Server time: "+time.getTime());
		// session ID
		String sessionID=SessionIDInHandler.getSessionID();
		assertNotNull(sessionID);
		System.out.println("Have session ID: "+sessionID);
		time=client.getCurrentTime();
		String sessionID2=SessionIDInHandler.getSessionID();
		assertNotNull(sessionID2);
		assertEquals(sessionID, sessionID2);
	}
	
	@Test
	public void testAddServerPublicKey() throws Exception {
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://test");
		WSUtilities.addServerPublicKey(epr, kernel.getSecurityManager().getServerCert());
		System.out.println(epr);
	}

}
