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
 

package eu.unicore.services.ws.testutils;


import static de.fzj.unicore.wsrflite.ContainerProperties.PREFIX;
import static de.fzj.unicore.wsrflite.ContainerProperties.SERVER_HOST;
import static de.fzj.unicore.wsrflite.ContainerProperties.SERVER_PORT;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.server.JettyServer;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public abstract class JettyTestCase {
	protected static boolean started = false;
	protected static Kernel kernel;
	protected static JettyServer server;
	
	@Before
	public void setUp()throws Exception{
		if (started)
			return;
		Properties properties = getServerSideSecurityProperties();
		FileUtils.deleteDirectory(new File("target","data"));
		properties.setProperty("persistence.directory", "target/data");
		properties.setProperty(PREFIX+SERVER_HOST, "localhost");
		properties.setProperty(PREFIX+SERVER_PORT, ""+getPort());
		init(properties);
		started = true;
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		if (kernel!=null && started)
			kernel.shutdown();
		started = false;
	}

	protected void init(Properties properties) throws Exception {
		kernel=new Kernel(properties);
		kernel.startSynchronous();
		server=kernel.getServer();
	}
	
	protected int getPort(){
		return 65321;
	}
	
	protected IClientConfiguration getClientSideSecurityProperties() throws Exception{
		DefaultClientConfiguration cdd=new DefaultClientConfiguration();
		cdd.setMessageLogging(true);
		cdd.setServerHostnameCheckingMode(ServerHostnameCheckingMode.NONE);
		return cdd;
	}
	
	protected Properties getServerSideSecurityProperties() throws Exception {
		Properties properties= TestConfigUtil.getInsecureProperties();

		return properties;
	}
	
	protected String getBaseurl(){
		return "http://localhost:"+getPort()+"/services";
	}
	
}
