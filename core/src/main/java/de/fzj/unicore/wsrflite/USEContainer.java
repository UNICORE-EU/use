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

package de.fzj.unicore.wsrflite;

import java.util.Calendar;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.Log;

/**
 * Main class, intended to run the USE container with the configured services.
 * 
 * @author schuller
 */
public class USEContainer {

	private static final Logger logger = Log.getLogger(Log.UNICORE,
			USEContainer.class);

	public final Calendar upSince = Calendar.getInstance();

	protected final String name;
	protected final Kernel kernel;
	
	public String getVersion() {
		return getVersion(getClass());
	}

	public static String getVersion(Class<?> versionOf) {
		String version = versionOf.getPackage().getImplementationVersion(); 
		return version != null ? version : "DEVELOPMENT";
	}

	public final String getHeader() {
		return (name == null ? "UNICORE Container (USE)" : name) +  
				 " version " + getVersion();
	}

	/**
	 * @param config - configuration properties
	 * @param name - the name of this container
	 */
	public USEContainer(Properties config, String name) throws Exception {
		this.kernel = new Kernel(config);
		this.name = name;
	}

	/**
	 * @param configFile
	 */
	public USEContainer(String configFile, String name) throws Exception {
		this.kernel = new Kernel(configFile);
		this.name = name;
	}

	public Kernel getKernel() {
		return kernel;
	}
	
	public String getConnectionStatus(){
		StringBuilder report = new StringBuilder();
		String newline = System.getProperty("line.separator");
		report.append(newline);
		report.append("Server status report");
		report.append(newline);
		report.append("************************");
		report.append(newline);
		report.append(kernel.getConnectionStatus());
		return report.toString();
	}

	public void startSynchronous()throws Exception{
		kernel.startSynchronous();
		printInfo();
	}

	private void printInfo(){
		String msg = getHeader() + 
		 (kernel.getContainerProperties().getBooleanValue(ContainerProperties.ON_STARTUP_SELFTEST)?
			getConnectionStatus() : "" );
		System.out.println(msg);
		logger.info(msg);
	}
	
	/**
	 * start the UAS
	 * 
	 * args: mandatory config file name
	 * 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("ERROR: Configuration file name path must be provided as the first argument. " +
					"Server NOT started.");
			System.exit(1);
		}
			
		try{
			System.out.println("Reading config from " + args[0]);
			String name = null;
			if (args.length > 1)
				name = args[1];
			USEContainer uas=new USEContainer(args[0], name);
			uas.startSynchronous();
		}catch(Exception ex){
			ex.printStackTrace();
			System.err.println("ERROR during server startup, server NOT started.");
			System.exit(1);
		}
	}
}
