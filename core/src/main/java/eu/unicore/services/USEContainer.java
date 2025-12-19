package eu.unicore.services;

import java.util.Calendar;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;

/**
 * USE main class
 *
 * @author schuller
 */
public class USEContainer {

	private static final Logger logger = Log.getLogger(Log.UNICORE, USEContainer.class);

	public final Calendar upSince = Calendar.getInstance();

	protected final String name;

	protected final Kernel kernel;

	public final String getHeader() {
		return "UNICORE Container (USE)" + " version " + Kernel.getVersion();
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

	public String getStatusReport(){
		StringBuilder report = new StringBuilder();
		report.append("SERVER STATUS");
		report.append(System.getProperty("line.separator"));
		report.append(kernel.getConnectionStatus());
		return report.toString();
	}

	public void startSynchronous()throws Exception{
		kernel.startSynchronous();
		printInfo();
	}

	private void printInfo(){
		StringBuilder msg = new StringBuilder();
		msg.append(getHeader());
		msg.append(System.getProperty("line.separator"));
		if(kernel.getContainerProperties().getBooleanValue(ContainerProperties.ON_STARTUP_SELFTEST)) {
			msg.append(getStatusReport());
			msg.append(System.getProperty("line.separator"));
		}
		System.out.println(msg);
		logger.info(msg);
	}

	/**
	 * start the container
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
			USEContainer use = new USEContainer(args[0], name);
			use.startSynchronous();
		}catch(Throwable ex){
			ex.printStackTrace();
			System.err.println("ERROR during server startup, server NOT started.");
			System.exit(1);
		}
	}
}