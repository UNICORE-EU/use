package eu.unicore.uas.pdp.argus;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.Logger;
import org.herasaf.xacml.core.SyntaxException;
import org.xml.sax.SAXException;

import eu.unicore.services.ThreadingServices;
import eu.unicore.uas.pdp.local.LocalPolicyStore;
import eu.unicore.uas.pdp.local.PolicyListener;
import eu.unicore.util.Log;

public class ArgusHerasafPolicyStore extends LocalPolicyStore {
	public static final Logger log = Log.getLogger(Log.SECURITY,
			ArgusHerasafPolicyStore.class);


	public ArgusHerasafPolicyStore(PolicyListener pdp,
			String configurationFile, Object notification, ThreadingServices threadingSrv)
			throws IOException, SyntaxException, JAXBException, SAXException {
		super(pdp, configurationFile, -1, threadingSrv);
		waitAndReload(notification);
	}

	private void waitAndReload(final Object notification) {
		Runnable r = new Runnable() {
			public void run() {
				synchronized (notification) {
					while (true) {
						try {
							notification.wait();
						} catch (InterruptedException e) {
							log.error("Error waiting to notification: ", e);
						}
						log.info("Local Argus XACML was modified, re-configuring.");
						try {
							reload();
						} catch (IOException e) {
							log.error("Error reading policy dir  ", e);
						} catch (SyntaxException e) {
							log.error("Error parsing XAML policies: "
									+ e.toString() + " "
									+ e.getCause().toString(), e);
						}
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.start();
		// ResourcePool.getExecutorService().execute(r);
	}

}
